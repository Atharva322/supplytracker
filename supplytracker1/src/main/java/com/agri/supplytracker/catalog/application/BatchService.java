package com.agri.supplytracker.catalog.application;

import com.agri.supplytracker.catalog.domain.*;
import com.agri.supplytracker.catalog.persistence.ProductBatchRepository;
import com.agri.supplytracker.platform.domain.*;
import com.agri.supplytracker.platform.persistence.*;
import com.agri.supplytracker.platform.security.AuthorizationService;
import com.agri.supplytracker.traceability.domain.*;
import com.agri.supplytracker.traceability.persistence.TraceabilityEventRepository;
import com.agri.supplytracker.organization.application.OrganizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class BatchService {
    private final ProductBatchRepository batches;
    private final TraceabilityEventRepository events;
    private final OutboxEventRepository outbox;
    private final IdempotencyRecordRepository idempotency;
    private final AuthorizationService authorization;
    private final BatchTransitionPolicy transitions;
    private final OrganizationService organizations;

    public BatchService(ProductBatchRepository batches, TraceabilityEventRepository events,
                        OutboxEventRepository outbox, IdempotencyRecordRepository idempotency,
                        AuthorizationService authorization, BatchTransitionPolicy transitions, OrganizationService organizations) {
        this.batches=batches; this.events=events; this.outbox=outbox; this.idempotency=idempotency;
        this.authorization=authorization; this.transitions=transitions; this.organizations=organizations;
    }

    @Transactional
    public ProductBatch create(String organizationId, String batchId, String productName, String productType,
                               BigDecimal quantity, String unit, LocalDate harvestDate, String facilityId,
                               String actor, String key) {
        authorization.requireMember(organizationId, actor);
        requireKey(key);
        String requestHash = IdempotencySupport.hash("batch.create", organizationId, batchId, productName, productType, quantity, unit, harvestDate, facilityId);
        Optional<IdempotencyRecord> replay = idempotency.findByActorAndKey(actor, key);
        if (replay.isPresent()) {
            IdempotencySupport.requireSameRequest(replay.get(), requestHash);
            return replayBatch(replay.get(),actor);
        }
        if (quantity == null || quantity.signum() <= 0) throw new IllegalArgumentException("Quantity must be positive");
        if (facilityId != null && !facilityId.isBlank()) organizations.requireFacility(organizationId,facilityId,actor);
        ProductBatch batch = batches.save(ProductBatch.builder().batchId(batchId).organizationId(organizationId)
            .productName(productName).productType(productType).quantity(quantity).unit(unit).harvestDate(harvestDate)
            .currentFacilityId(facilityId).custodianOrganizationId(organizationId).status(BatchStatus.HARVESTED)
            .qualityStatus(QualityStatus.PENDING).createdAt(Instant.now()).updatedAt(Instant.now()).build());
        appendEvent(batch, TraceEventType.BATCH_CREATED, actor, Map.of("status", batch.getStatus().name()));
        idempotency.save(IdempotencyRecord.builder().actor(actor).key(key).requestHash(requestHash).resourceType("BATCH").resourceId(batch.getId()).createdAt(Instant.now()).build());
        return batch;
    }

    public ProductBatch get(String batchId, String actor) {
        ProductBatch batch = batches.findByBatchId(batchId).orElseThrow(() -> new NoSuchElementException("Batch not found"));
        if (!authorization.isMember(batch.getOrganizationId(), actor) && !authorization.isMember(batch.getCustodianOrganizationId(), actor)
            && !authorization.isMember(batch.getPendingCustodianOrganizationId(), actor)) {
            throw new org.springframework.security.access.AccessDeniedException("Batch is outside your organization");
        }
        return batch;
    }

    public ProductBatch saveProjection(ProductBatch batch) { batch.setUpdatedAt(Instant.now()); return batches.save(batch); }

    public List<ProductBatch> list(String organizationId, String actor) {
        authorization.requireMember(organizationId, actor);
        return batches.findByOrganizationIdOrCustodianOrganizationIdOrPendingCustodianOrganizationId(organizationId,organizationId,organizationId);
    }

    public List<TraceabilityEvent> timeline(String batchId, String actor) {
        ProductBatch batch = get(batchId, actor);
        return events.findByBatchIdOrderBySequenceNumberAsc(batch.getBatchId());
    }

    @Transactional
    public ProductBatch transition(String batchId, BatchStatus target, Long expectedVersion, String actor, String key) {
        requireKey(key);
        String requestHash = IdempotencySupport.hash("batch.transition", batchId, target, expectedVersion);
        Optional<IdempotencyRecord> replay = idempotency.findByActorAndKey(actor, key);
        if (replay.isPresent()) {
            IdempotencySupport.requireSameRequest(replay.get(), requestHash);
            return replayBatch(replay.get(),actor);
        }
        ProductBatch batch = get(batchId, actor);
        if (expectedVersion != null && !Objects.equals(batch.getVersion(), expectedVersion)) {
            throw new IllegalStateException("Batch version conflict");
        }
        transitions.validate(batch.getStatus(), target);
        batch.setStatus(target);
        if (target == BatchStatus.QUALITY_APPROVED) batch.setQualityStatus(QualityStatus.APPROVED);
        if (target == BatchStatus.REJECTED) batch.setQualityStatus(QualityStatus.REJECTED);
        batch.setUpdatedAt(Instant.now());
        ProductBatch saved = batches.save(batch);
        appendEvent(saved, TraceEventType.STATUS_CHANGED, actor, Map.of("status", target.name()));
        idempotency.save(IdempotencyRecord.builder().actor(actor).key(key).requestHash(requestHash).resourceType("BATCH").resourceId(saved.getId()).createdAt(Instant.now()).build());
        return saved;
    }

    public TraceabilityEvent appendEvent(ProductBatch batch, TraceEventType type, String actor, Map<String,String> metadata) {
        long sequence = events.findTopByBatchIdOrderBySequenceNumberDesc(batch.getBatchId())
            .map(last -> last.getSequenceNumber() + 1).orElse(1L);
        TraceabilityEvent event = events.save(TraceabilityEvent.builder().batchId(batch.getBatchId()).sequenceNumber(sequence)
            .type(type).organizationId(batch.getOrganizationId()).actor(actor).occurredAt(Instant.now()).metadata(metadata).build());
        outbox.save(OutboxEvent.builder().aggregateType("ProductBatch").aggregateId(batch.getBatchId())
            .eventType(type.name()).payload(metadata).createdAt(Instant.now()).build());
        return event;
    }

    @Transactional
    public ProductBatch importLegacy(ProductBatch batch, String actor, List<Map<String,String>> legacyStages) {
        ProductBatch saved=batches.save(batch);
        appendEvent(saved,TraceEventType.LEGACY_IMPORTED,actor,Map.of("sourceProductId",String.valueOf(saved.getMigrationSourceId())));
        for(Map<String,String> stage:legacyStages) appendEvent(saved,TraceEventType.LEGACY_IMPORTED,actor,stage);
        return saved;
    }

    private void requireKey(String key) { if (key == null || key.isBlank()) throw new IllegalArgumentException("Idempotency-Key header is required"); }
    private ProductBatch replayBatch(IdempotencyRecord record,String actor) {
        if(!"BATCH".equals(record.getResourceType())) throw new IllegalStateException("Idempotency key already used by another command");
        ProductBatch batch=batches.findById(record.getResourceId()).orElseThrow();
        if(!authorization.isMember(batch.getOrganizationId(),actor)&&!authorization.isMember(batch.getCustodianOrganizationId(),actor)
            &&!authorization.isMember(batch.getPendingCustodianOrganizationId(),actor))
            throw new org.springframework.security.access.AccessDeniedException("Batch is outside your organization");
        return batch;
    }
}
