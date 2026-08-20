package com.agri.supplytracker.lineage;

import com.agri.supplytracker.catalog.application.BatchService;
import com.agri.supplytracker.catalog.domain.ProductBatch;
import com.agri.supplytracker.catalog.persistence.ProductBatchRepository;
import com.agri.supplytracker.lineage.application.*;
import com.agri.supplytracker.lineage.domain.*;
import com.agri.supplytracker.lineage.persistence.RecallCaseRepository;
import com.agri.supplytracker.organization.application.OrganizationService;
import com.agri.supplytracker.platform.persistence.*;
import com.agri.supplytracker.platform.security.AuthorizationService;
import com.agri.supplytracker.service.NotificationService;
import com.agri.supplytracker.shipment.domain.*;
import com.agri.supplytracker.shipment.persistence.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecallServiceTest {
    @Test
    void simulationCreatesRepeatableScopeWithoutRealNotifications() {
        Fixture fixture = fixture();
        RecallCase recall = fixture.service.create("A", "possible contamination", true, "manager", "key-1");

        assertEquals(RecallStatus.OPEN, recall.getStatus());
        assertTrue(recall.isSimulation());
        assertEquals(List.of("A", "B"), recall.getScope().getAffectedBatchIds());
        assertEquals(List.of("shipment-1"), recall.getScope().getAffectedShipmentIds());
        assertEquals(List.of("facility-1", "facility-2"), recall.getScope().getAffectedFacilityIds());
        assertEquals("DERIVE from A", recall.getScope().getExplanations().get("B"));
        assertTrue(recall.getNotices().stream().allMatch(RecallNotice::isSimulation));
        verifyNoInteractions(fixture.notifications);

        RecallCase replayShape = fixture.service.create("A", "possible contamination", true, "manager", "key-2");
        assertEquals(recall.getScope().getAffectedBatchIds(), replayShape.getScope().getAffectedBatchIds());
        assertEquals(recall.getScope().getAffectedShipmentIds(), replayShape.getScope().getAffectedShipmentIds());
    }

    @Test
    void liveRecallDeduplicatesNotificationRecipients() {
        Fixture fixture = fixture();
        fixture.service.create("A", "confirmed issue", false, "manager", "key-live");

        ArgumentCaptor<Collection<String>> recipients = ArgumentCaptor.forClass(Collection.class);
        verify(fixture.notifications).notifyUsers(recipients.capture(), eq("RECALL_CASE_CREATED"), anyString(), anyString(), anyString());
        assertEquals(Set.of("shared-user", "org2-user"), new TreeSet<>(recipients.getValue()));
    }

    private Fixture fixture() {
        RecallCaseRepository recalls = mock(RecallCaseRepository.class);
        ProductBatchRepository batches = mock(ProductBatchRepository.class);
        ShipmentRepository shipments = mock(ShipmentRepository.class);
        IdempotencyRecordRepository idempotency = mock(IdempotencyRecordRepository.class);
        OutboxEventRepository outbox = mock(OutboxEventRepository.class);
        AuthorizationService authorization = mock(AuthorizationService.class);
        OrganizationService organizations = mock(OrganizationService.class);
        NotificationService notifications = mock(NotificationService.class);
        BatchService batchService = mock(BatchService.class);
        LineageService lineage = mock(LineageService.class);
        RecallService service = new RecallService(recalls, batches, shipments, idempotency, outbox, authorization, organizations, notifications, batchService, lineage);
        when(idempotency.findByActorAndKey(anyString(), anyString())).thenReturn(Optional.empty());
        when(batchService.get("A", "manager")).thenReturn(batch("A", "org1", "facility-1"));
        when(lineage.traverseDownstream("A")).thenReturn(new LineageService.TraversalResult(
            new LinkedHashSet<>(List.of("A", "B")),
            Map.of("A", "source batch", "B", "DERIVE from A"),
            RecallTraversalStats.builder().nodesVisited(2).edgesVisited(1).maxDepthReached(1).build()));
        when(batches.findByBatchIdIn(anyCollection())).thenReturn(List.of(batch("B", "org2", "facility-2"), batch("A", "org1", "facility-1")));
        when(shipments.findByLinesBatchIdIn(anyCollection())).thenReturn(List.of(Shipment.builder().id("shipment-1")
            .senderOrganizationId("org1").recipientOrganizationId("org2")
            .lines(List.of(ShipmentLine.builder().batchId("B").quantity(BigDecimal.ONE).unit("kg").build())).build()));
        when(organizations.memberUsernames("org1")).thenReturn(List.of("shared-user"));
        when(organizations.memberUsernames("org2")).thenReturn(List.of("shared-user", "org2-user"));
        when(recalls.save(any())).thenAnswer(invocation -> {
            RecallCase recall = invocation.getArgument(0);
            recall.setId(UUID.randomUUID().toString());
            return recall;
        });
        return new Fixture(service, notifications);
    }

    private ProductBatch batch(String batchId, String organizationId, String facilityId) {
        return ProductBatch.builder().id(batchId + "-id").batchId(batchId).organizationId(organizationId)
            .custodianOrganizationId(organizationId).currentFacilityId(facilityId).quantity(BigDecimal.ONE).unit("kg").build();
    }

    private record Fixture(RecallService service, NotificationService notifications) {}
}
