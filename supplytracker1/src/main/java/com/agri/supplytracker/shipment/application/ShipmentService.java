package com.agri.supplytracker.shipment.application;

import com.agri.supplytracker.catalog.application.BatchService;
import com.agri.supplytracker.catalog.domain.*;
import com.agri.supplytracker.platform.domain.IdempotencyRecord;
import com.agri.supplytracker.platform.domain.IdempotencySupport;
import com.agri.supplytracker.platform.persistence.IdempotencyRecordRepository;
import com.agri.supplytracker.platform.security.AuthorizationService;
import com.agri.supplytracker.organization.application.OrganizationService;
import com.agri.supplytracker.service.NotificationService;
import com.agri.supplytracker.shipment.domain.*;
import com.agri.supplytracker.shipment.persistence.*;
import com.agri.supplytracker.traceability.domain.TraceEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class ShipmentService {
    private final ShipmentRepository shipments; private final SensorReadingRepository readings;
    private final ColdChainIncidentRepository incidents; private final CustodyTransferRepository transfers;
    private final CustodyService custody; private final BatchService batches; private final AuthorizationService authorization;
    private final IdempotencyRecordRepository idempotency;
    private final OrganizationService organizations; private final NotificationService notifications;

    public ShipmentService(ShipmentRepository shipments, SensorReadingRepository readings, ColdChainIncidentRepository incidents,
                           CustodyTransferRepository transfers, CustodyService custody, BatchService batches,
                           AuthorizationService authorization, IdempotencyRecordRepository idempotency,
                           OrganizationService organizations, NotificationService notifications) {
        this.shipments=shipments; this.readings=readings; this.incidents=incidents; this.transfers=transfers;
        this.custody=custody; this.batches=batches; this.authorization=authorization; this.idempotency=idempotency;
        this.organizations=organizations; this.notifications=notifications;
    }

    @Transactional
    public Shipment create(String transferId, BigDecimal minC, BigDecimal maxC, String actor, String key) {
        requireKey(key); String requestHash=IdempotencySupport.hash("shipment.create",transferId,minC,maxC); Optional<IdempotencyRecord> replay=idempotency.findByActorAndKey(actor,key); if(replay.isPresent()){IdempotencySupport.requireSameRequest(replay.get(),requestHash); return replay(replay.get(),actor);}
        CustodyTransfer transfer = custody.get(transferId);
        authorization.requireMember(transfer.getSenderOrganizationId(), actor);
        if (transfer.getStatus()!=CustodyTransfer.Status.ACCEPTED) throw new IllegalStateException("Custody must be accepted before shipment creation");
        if (minC == null || maxC == null || minC.compareTo(maxC) >= 0) throw new IllegalArgumentException("Invalid cold-chain temperature range");
        ProductBatch batch=batches.get(transfer.getBatchId(), actor);
        if (batch.getQualityStatus()!=QualityStatus.APPROVED || batch.getStatus()!=BatchStatus.READY_FOR_SHIPMENT) throw new IllegalStateException("Batch must be quality-approved and ready for shipment");
        Shipment shipment=shipments.save(Shipment.builder().custodyTransferId(transferId).senderOrganizationId(transfer.getSenderOrganizationId())
            .recipientOrganizationId(transfer.getRecipientOrganizationId()).lines(List.of(ShipmentLine.builder().batchId(transfer.getBatchId()).quantity(transfer.getQuantity()).unit(transfer.getUnit()).build()))
            .minimumTemperatureC(minC).maximumTemperatureC(maxC).status(Shipment.Status.DRAFT).createdAt(Instant.now()).build());
        idempotency.save(IdempotencyRecord.builder().actor(actor).key(key).requestHash(requestHash).resourceType("SHIPMENT").resourceId(shipment.getId()).createdAt(Instant.now()).build());
        return shipment;
    }

    @Transactional
    public Shipment dispatch(String shipmentId, String actor, String key) {
        requireKey(key); String requestHash=IdempotencySupport.hash("shipment.dispatch",shipmentId); Optional<IdempotencyRecord> replay=idempotency.findByActorAndKey(actor,key); if(replay.isPresent()){IdempotencySupport.requireSameRequest(replay.get(),requestHash); return replay(replay.get(),actor);}
        Shipment shipment=getAuthorized(shipmentId, actor); authorization.requireMember(shipment.getSenderOrganizationId(), actor);
        if(shipment.getStatus()!=Shipment.Status.DRAFT) throw new IllegalStateException("Only draft shipments can be dispatched");
        shipment.setStatus(Shipment.Status.IN_TRANSIT); shipment.setDispatchedAt(Instant.now()); Shipment saved=shipments.save(shipment);
        for(ShipmentLine line:shipment.getLines()) {
            ProductBatch batch=batches.get(line.getBatchId(), actor);
            if(batch.getQualityStatus()!=QualityStatus.APPROVED || batch.getStatus()!=BatchStatus.READY_FOR_SHIPMENT) throw new IllegalStateException("Batch is no longer ready for shipment");
            batch.setStatus(BatchStatus.IN_TRANSIT); batches.saveProjection(batch);
            batches.appendEvent(batch, TraceEventType.SHIPMENT_DISPATCHED, actor, Map.of("shipmentId", shipmentId));
        }
        idempotency.save(IdempotencyRecord.builder().actor(actor).key(key).requestHash(requestHash).resourceType("SHIPMENT").resourceId(saved.getId()).createdAt(Instant.now()).build()); return saved;
    }

    @Transactional
    public SensorReading ingest(String shipmentId, String readingId, String deviceId, BigDecimal temperatureC,
                                BigDecimal humidity, Instant observedAt, String actor) {
        Shipment shipment=getAuthorized(shipmentId,actor);
        if(shipment.getStatus()!=Shipment.Status.IN_TRANSIT) throw new IllegalStateException("Sensor readings require an in-transit shipment");
        Optional<SensorReading> replay=readings.findByReadingId(readingId);
        if(replay.isPresent()) {
            if(!shipmentId.equals(replay.get().getShipmentId())) throw new IllegalStateException("Sensor reading ID already belongs to another shipment");
            return replay.get();
        }
        SensorReading reading=readings.save(SensorReading.builder().readingId(readingId).shipmentId(shipmentId).deviceId(deviceId)
            .temperatureC(temperatureC).humidityPercent(humidity).observedAt(observedAt).receivedAt(Instant.now()).build());
        if(temperatureC.compareTo(shipment.getMinimumTemperatureC())<0 || temperatureC.compareTo(shipment.getMaximumTemperatureC())>0) {
            CustodyTransfer transfer=custody.get(shipment.getCustodyTransferId());
            for(ShipmentLine line:shipment.getLines()) {
                incidents.save(ColdChainIncident.builder().shipmentId(shipmentId).readingId(readingId).batchId(line.getBatchId())
                    .temperatureC(temperatureC).allowedMinimumC(shipment.getMinimumTemperatureC()).allowedMaximumC(shipment.getMaximumTemperatureC())
                    .detectedAt(Instant.now()).status("OPEN").build());
                ProductBatch batch=batches.get(line.getBatchId(), transfer.getOfferedBy());
                batches.appendEvent(batch, TraceEventType.COLD_CHAIN_EXCURSION, actor, Map.of("shipmentId",shipmentId,"readingId",readingId,"temperatureC",temperatureC.toPlainString()));
            }
            List<String> audience=new ArrayList<>(); audience.addAll(organizations.memberUsernames(shipment.getSenderOrganizationId()));
            audience.addAll(organizations.memberUsernames(shipment.getRecipientOrganizationId()));
            notifications.notifyUsers(audience,"COLD_CHAIN_EXCURSION","Cold-chain excursion detected",
                "Shipment " + shipmentId + " reported " + temperatureC.toPlainString() + "°C",shipmentId);
        }
        return reading;
    }

    @Transactional
    public Shipment receive(String shipmentId, String actor, String key) {
        requireKey(key); String requestHash=IdempotencySupport.hash("shipment.receive",shipmentId); Optional<IdempotencyRecord> replay=idempotency.findByActorAndKey(actor,key); if(replay.isPresent()){IdempotencySupport.requireSameRequest(replay.get(),requestHash); return replay(replay.get(),actor);}
        Shipment shipment=getAuthorized(shipmentId,actor); authorization.requireMember(shipment.getRecipientOrganizationId(),actor);
        if(shipment.getStatus()!=Shipment.Status.IN_TRANSIT) throw new IllegalStateException("Only in-transit shipments can be received");
        shipment.setStatus(Shipment.Status.DELIVERED); shipment.setReceivedAt(Instant.now()); Shipment saved=shipments.save(shipment);
        CustodyTransfer transfer=custody.get(shipment.getCustodyTransferId()); transfer.setStatus(CustodyTransfer.Status.COMPLETED); transfer.setCompletedAt(Instant.now()); transfers.save(transfer);
        for(ShipmentLine line:shipment.getLines()) {
            ProductBatch batch=batches.get(line.getBatchId(), transfer.getOfferedBy());
            if(batch.getStatus()!=BatchStatus.IN_TRANSIT) throw new IllegalStateException("Batch is not in transit");
            batch.setStatus(BatchStatus.DELIVERED);
            batch.setCustodianOrganizationId(shipment.getRecipientOrganizationId()); batch.setPendingCustodianOrganizationId(null);
            batch.setActiveCustodyTransferId(null); batches.saveProjection(batch);
            batches.appendEvent(batch, TraceEventType.SHIPMENT_RECEIVED, actor, Map.of("shipmentId",shipmentId));
        }
        idempotency.save(IdempotencyRecord.builder().actor(actor).key(key).requestHash(requestHash).resourceType("SHIPMENT").resourceId(saved.getId()).createdAt(Instant.now()).build()); return saved;
    }

    public Shipment getAuthorized(String id,String actor) { Shipment s=shipments.findById(id).orElseThrow(() -> new NoSuchElementException("Shipment not found"));
        if(!authorization.isMember(s.getSenderOrganizationId(),actor)&&!authorization.isMember(s.getRecipientOrganizationId(),actor)) throw new org.springframework.security.access.AccessDeniedException("Shipment outside your organization"); return s; }
    public List<SensorReading> readings(String shipmentId,String actor){getAuthorized(shipmentId,actor);return readings.findByShipmentIdOrderByObservedAtAsc(shipmentId);}
    public List<ColdChainIncident> incidents(String shipmentId,String actor){getAuthorized(shipmentId,actor);return incidents.findByShipmentId(shipmentId);}
    private Shipment replay(IdempotencyRecord r,String actor){if(!"SHIPMENT".equals(r.getResourceType()))throw new IllegalStateException("Idempotency key already used by another command");return getAuthorized(r.getResourceId(),actor);}
    private void requireKey(String key){if(key==null||key.isBlank())throw new IllegalArgumentException("Idempotency-Key header is required");}
}
