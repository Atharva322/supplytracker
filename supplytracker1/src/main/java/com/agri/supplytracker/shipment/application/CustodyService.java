package com.agri.supplytracker.shipment.application;

import com.agri.supplytracker.catalog.application.BatchService;
import com.agri.supplytracker.catalog.domain.ProductBatch;
import com.agri.supplytracker.organization.persistence.OrganizationRepository;
import com.agri.supplytracker.platform.domain.IdempotencyRecord;
import com.agri.supplytracker.platform.domain.IdempotencySupport;
import com.agri.supplytracker.platform.persistence.IdempotencyRecordRepository;
import com.agri.supplytracker.platform.security.AuthorizationService;
import com.agri.supplytracker.shipment.domain.CustodyTransfer;
import com.agri.supplytracker.shipment.persistence.CustodyTransferRepository;
import com.agri.supplytracker.traceability.domain.TraceEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class CustodyService {
    private final CustodyTransferRepository transfers;
    private final BatchService batches;
    private final AuthorizationService authorization;
    private final OrganizationRepository organizations;
    private final IdempotencyRecordRepository idempotency;

    public CustodyService(CustodyTransferRepository transfers, BatchService batches, AuthorizationService authorization,
                          OrganizationRepository organizations, IdempotencyRecordRepository idempotency) {
        this.transfers=transfers; this.batches=batches; this.authorization=authorization;
        this.organizations=organizations; this.idempotency=idempotency;
    }

    @Transactional
    public CustodyTransfer offer(String batchId, String recipientOrganizationId, BigDecimal quantity, String unit, String actor, String key) {
        requireKey(key);
        String requestHash = IdempotencySupport.hash("custody.offer", batchId, recipientOrganizationId, quantity, unit);
        Optional<IdempotencyRecord> replay = idempotency.findByActorAndKey(actor, key);
        if (replay.isPresent()) { IdempotencySupport.requireSameRequest(replay.get(), requestHash); CustodyTransfer previous=replay("CUSTODY", replay.get()); authorization.requireMember(previous.getSenderOrganizationId(),actor); return previous; }
        ProductBatch batch = batches.get(batchId, actor);
        String sender = batch.getCustodianOrganizationId();
        authorization.requireMember(sender, actor);
        if (batch.getActiveCustodyTransferId()!=null) throw new IllegalStateException("Batch already has an active custody transfer");
        if (sender.equals(recipientOrganizationId)) throw new IllegalArgumentException("Recipient must be a different organization");
        if (!organizations.existsById(recipientOrganizationId)) throw new IllegalArgumentException("Recipient organization not found");
        if (quantity == null || quantity.signum() <= 0 || quantity.compareTo(batch.getQuantity()) != 0) throw new IllegalArgumentException("Custody currently requires the full batch quantity; split/merge is a later lineage phase");
        if (!Objects.equals(unit, batch.getUnit())) throw new IllegalArgumentException("Transfer unit must match batch unit");
        CustodyTransfer transfer = transfers.save(CustodyTransfer.builder().batchId(batchId).senderOrganizationId(sender)
            .recipientOrganizationId(recipientOrganizationId).quantity(quantity).unit(unit).status(CustodyTransfer.Status.OFFERED)
            .offeredBy(actor).offeredAt(Instant.now()).build());
        batch.setActiveCustodyTransferId(transfer.getId()); batches.saveProjection(batch);
        batches.appendEvent(batch, TraceEventType.CUSTODY_OFFERED, actor, Map.of("transferId", transfer.getId(), "recipientOrganizationId", recipientOrganizationId));
        idempotency.save(IdempotencyRecord.builder().actor(actor).key(key).requestHash(requestHash).resourceType("CUSTODY").resourceId(transfer.getId()).createdAt(Instant.now()).build());
        return transfer;
    }

    @Transactional
    public CustodyTransfer accept(String transferId, String actor, String key) {
        requireKey(key);
        String requestHash = IdempotencySupport.hash("custody.accept", transferId);
        Optional<IdempotencyRecord> replay = idempotency.findByActorAndKey(actor, key);
        if (replay.isPresent()) { IdempotencySupport.requireSameRequest(replay.get(), requestHash); CustodyTransfer previous=replay("CUSTODY", replay.get()); authorization.requireMember(previous.getRecipientOrganizationId(),actor); return previous; }
        CustodyTransfer transfer = get(transferId);
        authorization.requireMember(transfer.getRecipientOrganizationId(), actor);
        if (transfer.getStatus() != CustodyTransfer.Status.OFFERED) throw new IllegalStateException("Only offered custody can be accepted");
        transfer.setStatus(CustodyTransfer.Status.ACCEPTED); transfer.setAcceptedBy(actor); transfer.setAcceptedAt(Instant.now());
        CustodyTransfer saved = transfers.save(transfer);
        ProductBatch batch = batches.get(transfer.getBatchId(), transfer.getOfferedBy());
        if(!transferId.equals(batch.getActiveCustodyTransferId())) throw new IllegalStateException("Custody transfer is no longer active for this batch");
        batch.setPendingCustodianOrganizationId(transfer.getRecipientOrganizationId());
        batches.saveProjection(batch);
        batches.appendEvent(batch, TraceEventType.CUSTODY_ACCEPTED, actor, Map.of("transferId", transferId, "recipientOrganizationId", transfer.getRecipientOrganizationId()));
        idempotency.save(IdempotencyRecord.builder().actor(actor).key(key).requestHash(requestHash).resourceType("CUSTODY").resourceId(saved.getId()).createdAt(Instant.now()).build());
        return saved;
    }

    public CustodyTransfer get(String id) { return transfers.findById(id).orElseThrow(() -> new NoSuchElementException("Custody transfer not found")); }
    private CustodyTransfer replay(String type, IdempotencyRecord record) {
        if (!type.equals(record.getResourceType())) throw new IllegalStateException("Idempotency key already used by another command");
        return get(record.getResourceId());
    }
    private void requireKey(String key) { if (key == null || key.isBlank()) throw new IllegalArgumentException("Idempotency-Key header is required"); }
}
