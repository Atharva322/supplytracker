package com.agri.supplytracker.lineage.application;

import com.agri.supplytracker.catalog.application.BatchService;
import com.agri.supplytracker.catalog.domain.ProductBatch;
import com.agri.supplytracker.catalog.persistence.ProductBatchRepository;
import com.agri.supplytracker.lineage.domain.*;
import com.agri.supplytracker.lineage.persistence.RecallCaseRepository;
import com.agri.supplytracker.organization.application.OrganizationService;
import com.agri.supplytracker.platform.domain.*;
import com.agri.supplytracker.platform.persistence.*;
import com.agri.supplytracker.platform.security.AuthorizationService;
import com.agri.supplytracker.service.NotificationService;
import com.agri.supplytracker.shipment.domain.Shipment;
import com.agri.supplytracker.shipment.persistence.ShipmentRepository;
import com.agri.supplytracker.traceability.domain.TraceEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class RecallService {
    private final RecallCaseRepository recalls;
    private final ProductBatchRepository batches;
    private final ShipmentRepository shipments;
    private final IdempotencyRecordRepository idempotency;
    private final OutboxEventRepository outbox;
    private final AuthorizationService authorization;
    private final OrganizationService organizations;
    private final NotificationService notifications;
    private final BatchService batchService;
    private final LineageService lineage;

    public RecallService(RecallCaseRepository recalls, ProductBatchRepository batches, ShipmentRepository shipments,
                         IdempotencyRecordRepository idempotency, OutboxEventRepository outbox,
                         AuthorizationService authorization, OrganizationService organizations, NotificationService notifications,
                         BatchService batchService, LineageService lineage) {
        this.recalls = recalls; this.batches = batches; this.shipments = shipments; this.idempotency = idempotency; this.outbox = outbox;
        this.authorization = authorization; this.organizations = organizations; this.notifications = notifications;
        this.batchService = batchService; this.lineage = lineage;
    }

    @Transactional
    public RecallCase create(String sourceBatchId, String reason, boolean simulation, String actor, String key) {
        requireKey(key);
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason is required");
        ProductBatch source = batchService.get(sourceBatchId, actor);
        authorization.requireManager(source.getOrganizationId(), actor);
        String requestHash = IdempotencySupport.hash("recall.create", sourceBatchId, reason, simulation);
        Optional<IdempotencyRecord> replay = idempotency.findByActorAndKey(actor, key);
        if (replay.isPresent()) { IdempotencySupport.requireSameRequest(replay.get(), requestHash); return get(replay.get().getResourceId(), actor); }
        LineageService.TraversalResult traversal = lineage.traverseDownstream(sourceBatchId);
        RecallScopeSnapshot scope = snapshot(traversal);
        List<RecallNotice> notices = simulation ? simulatedNotices(scope) : notifyAffected(scope);
        Instant now = Instant.now();
        RecallCase recall = recalls.save(RecallCase.builder().sourceBatchId(sourceBatchId).organizationId(source.getOrganizationId())
            .reason(reason).simulation(simulation).status(RecallStatus.OPEN).scope(scope).traversalStats(traversal.stats())
            .actions(List.of(RecallAction.builder().action(simulation ? "SIMULATED" : "CREATED").actor(actor).note(reason).createdAt(now).build()))
            .notices(notices).acknowledgments(List.of()).createdBy(actor).createdAt(now).build());
        outbox.save(OutboxEvent.builder().aggregateType("RecallCase").aggregateId(recall.getId())
            .eventType("RECALL_CASE_CREATED").payload(Map.of("sourceBatchId", sourceBatchId, "simulation", String.valueOf(simulation)))
            .createdAt(now).build());
        batchService.appendEvent(source, TraceEventType.RECALL_CASE_CREATED, actor, Map.of("recallCaseId", recall.getId()));
        idempotency.save(IdempotencyRecord.builder().actor(actor).key(key).requestHash(requestHash).resourceType("RECALL_CASE").resourceId(recall.getId()).createdAt(now).build());
        return recall;
    }

    public RecallCase get(String recallId, String actor) {
        RecallCase recall = recalls.findById(recallId).orElseThrow(() -> new NoSuchElementException("Recall case not found"));
        authorization.requireMember(recall.getOrganizationId(), actor);
        return recall;
    }

    public List<RecallCase> list(String organizationId, String actor) {
        authorization.requireManager(organizationId, actor);
        return recalls.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
    }

    @Transactional
    public RecallCase acknowledge(String recallId, String organizationId, String note, String actor) {
        RecallCase recall = get(recallId, actor);
        if (!recall.getScope().getAffectedOrganizationIds().contains(organizationId)) throw new IllegalArgumentException("Organization is not in recall scope");
        authorization.requireMember(organizationId, actor);
        List<RecallAcknowledgment> acknowledgments = new ArrayList<>(optional(recall.getAcknowledgments()));
        acknowledgments.add(RecallAcknowledgment.builder().organizationId(organizationId).actor(actor).note(note).acknowledgedAt(Instant.now()).build());
        recall.setAcknowledgments(acknowledgments);
        return recalls.save(recall);
    }

    @Transactional
    public RecallCase resolve(String recallId, String resolution, String actor) {
        RecallCase recall = get(recallId, actor);
        authorization.requireManager(recall.getOrganizationId(), actor);
        if (resolution == null || resolution.isBlank()) throw new IllegalArgumentException("resolution is required");
        recall.setStatus(RecallStatus.RESOLVED); recall.setResolvedBy(actor); recall.setResolution(resolution); recall.setResolvedAt(Instant.now());
        List<RecallAction> actions = new ArrayList<>(optional(recall.getActions()));
        actions.add(RecallAction.builder().action("RESOLVED").actor(actor).note(resolution).createdAt(recall.getResolvedAt()).build());
        recall.setActions(actions);
        RecallCase saved = recalls.save(recall);
        outbox.save(OutboxEvent.builder().aggregateType("RecallCase").aggregateId(saved.getId()).eventType("RECALL_CASE_RESOLVED")
            .payload(Map.of("recallCaseId", saved.getId())).createdAt(Instant.now()).build());
        return saved;
    }

    private RecallScopeSnapshot snapshot(LineageService.TraversalResult traversal) {
        List<String> batchIds = sorted(traversal.batchIds());
        List<ProductBatch> affectedBatches = batches.findByBatchIdIn(batchIds);
        List<Shipment> affectedShipments = shipments.findByLinesBatchIdIn(batchIds);
        Set<String> facilities = new TreeSet<>(); Set<String> organizationsSeen = new TreeSet<>();
        Set<String> holders = new TreeSet<>(); Set<String> recipients = new TreeSet<>(); Set<String> shipmentIds = new TreeSet<>();
        for (ProductBatch batch : affectedBatches) {
            addIfPresent(facilities, batch.getCurrentFacilityId());
            addIfPresent(organizationsSeen, batch.getOrganizationId()); addIfPresent(organizationsSeen, batch.getCustodianOrganizationId()); addIfPresent(organizationsSeen, batch.getPendingCustodianOrganizationId());
            addIfPresent(holders, batch.getCustodianOrganizationId());
        }
        for (Shipment shipment : affectedShipments) {
            addIfPresent(shipmentIds, shipment.getId()); addIfPresent(organizationsSeen, shipment.getSenderOrganizationId());
            addIfPresent(organizationsSeen, shipment.getRecipientOrganizationId()); addIfPresent(recipients, shipment.getRecipientOrganizationId());
        }
        return RecallScopeSnapshot.builder().affectedBatchIds(batchIds).affectedShipmentIds(sorted(shipmentIds))
            .affectedFacilityIds(sorted(facilities)).affectedOrganizationIds(sorted(organizationsSeen))
            .inventoryHolderOrganizationIds(sorted(holders)).recipientOrganizationIds(sorted(recipients))
            .explanations(new TreeMap<>(traversal.explanations())).capturedAt(Instant.now()).build();
    }

    private List<RecallNotice> notifyAffected(RecallScopeSnapshot scope) {
        Set<String> usernames = new TreeSet<>(); List<RecallNotice> notices = new ArrayList<>(); Instant now = Instant.now();
        for (String organizationId : scope.getAffectedOrganizationIds()) {
            for (String username : organizations.memberUsernames(organizationId)) {
                if (usernames.add(username)) notices.add(RecallNotice.builder().organizationId(organizationId).recipient(username).sentAt(now).simulation(false).build());
            }
        }
        if (!usernames.isEmpty()) notifications.notifyUsers(usernames, "RECALL_CASE_CREATED", "Recall scope generated", "A recall case affects your organization", String.join(",", scope.getAffectedBatchIds()));
        return notices;
    }

    private List<RecallNotice> simulatedNotices(RecallScopeSnapshot scope) {
        List<RecallNotice> notices = new ArrayList<>(); Instant now = Instant.now();
        for (String organizationId : scope.getAffectedOrganizationIds()) {
            for (String username : organizations.memberUsernames(organizationId)) notices.add(RecallNotice.builder().organizationId(organizationId).recipient(username).sentAt(now).simulation(true).build());
        }
        return notices;
    }

    private List<String> sorted(Collection<String> values) { return values.stream().filter(Objects::nonNull).filter(value -> !value.isBlank()).distinct().sorted().toList(); }
    private <T> List<T> optional(List<T> values) { return values == null ? List.of() : values; }
    private void addIfPresent(Set<String> target, String value) { if (value != null && !value.isBlank()) target.add(value); }
    private void requireKey(String key) { if (key == null || key.isBlank()) throw new IllegalArgumentException("Idempotency-Key header is required"); }
}
