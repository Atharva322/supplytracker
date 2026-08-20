package com.agri.supplytracker.lineage;

import com.agri.supplytracker.catalog.application.BatchService;
import com.agri.supplytracker.catalog.domain.ProductBatch;
import com.agri.supplytracker.lineage.application.LineageService;
import com.agri.supplytracker.lineage.domain.*;
import com.agri.supplytracker.lineage.persistence.LineageEdgeRepository;
import com.agri.supplytracker.platform.persistence.IdempotencyRecordRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LineageServiceTest {
    @Test
    void splitRequiresQuantityConservation() {
        LineageEdgeRepository edges = mock(LineageEdgeRepository.class);
        IdempotencyRecordRepository idempotency = mock(IdempotencyRecordRepository.class);
        BatchService batches = mock(BatchService.class);
        LineageService service = new LineageService(edges, idempotency, batches, 12, 1000);
        when(idempotency.findByActorAndKey("alice", "key-1")).thenReturn(Optional.empty());
        when(batches.get("PARENT", "alice")).thenReturn(batch("PARENT", "org1", "10.0", "kg"));
        when(batches.get("CHILD-A", "alice")).thenReturn(batch("CHILD-A", "org1", "4.0", "kg"));
        when(batches.get("CHILD-B", "alice")).thenReturn(batch("CHILD-B", "org1", "6.0", "kg"));
        when(edges.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<LineageEdge> saved = service.split("PARENT", List.of(
            new LineageService.BatchQuantity("CHILD-A", new BigDecimal("4.0"), "kg"),
            new LineageService.BatchQuantity("CHILD-B", new BigDecimal("6.0"), "kg")), "alice", "key-1");

        assertEquals(2, saved.size());
        verify(edges, times(2)).save(any(LineageEdge.class));

        assertThrows(IllegalArgumentException.class, () -> service.split("PARENT", List.of(
            new LineageService.BatchQuantity("CHILD-A", new BigDecimal("3.0"), "kg"),
            new LineageService.BatchQuantity("CHILD-B", new BigDecimal("6.0"), "kg")), "alice", "key-2"));
    }

    @Test
    void deriveRejectsCycles() {
        LineageEdgeRepository edges = mock(LineageEdgeRepository.class);
        IdempotencyRecordRepository idempotency = mock(IdempotencyRecordRepository.class);
        BatchService batches = mock(BatchService.class);
        LineageService service = new LineageService(edges, idempotency, batches, 12, 1000);
        when(idempotency.findByActorAndKey("alice", "key-1")).thenReturn(Optional.empty());
        when(batches.get("PARENT", "alice")).thenReturn(batch("PARENT", "org1", "10.0", "kg"));
        when(batches.get("CHILD", "alice")).thenReturn(batch("CHILD", "org1", "3.0", "kg"));
        when(edges.findByParentBatchId("CHILD")).thenReturn(List.of(edge("CHILD", "PARENT")));

        assertThrows(IllegalStateException.class,
            () -> service.derive("PARENT", "CHILD", new BigDecimal("3.0"), "kg", "alice", "key-1"));
        verify(edges, never()).save(any());
    }

    @Test
    void traversalHandlesMultiHopAndDepthBound() {
        LineageEdgeRepository edges = mock(LineageEdgeRepository.class);
        LineageService service = new LineageService(edges, mock(IdempotencyRecordRepository.class), mock(BatchService.class), 12, 1000);
        when(edges.findByParentBatchId("A")).thenReturn(List.of(edge("A", "B")));
        when(edges.findByParentBatchId("B")).thenReturn(List.of(edge("B", "C")));
        when(edges.findByParentBatchId("C")).thenReturn(List.of());

        LineageService.TraversalResult result = service.traverseDownstream("A", 5, 100);

        assertEquals(Set.of("A", "B", "C"), result.batchIds());
        assertEquals(2, result.stats().getEdgesVisited());
        assertFalse(result.stats().isTruncated());

        LineageService.TraversalResult bounded = service.traverseDownstream("A", 1, 100);
        assertTrue(bounded.stats().isTruncated());
        assertEquals("max depth reached", bounded.stats().getTruncationReason());
    }

    private ProductBatch batch(String batchId, String organizationId, String quantity, String unit) {
        return ProductBatch.builder().id(batchId + "-id").batchId(batchId).organizationId(organizationId)
            .custodianOrganizationId(organizationId).quantity(new BigDecimal(quantity)).unit(unit).build();
    }

    private LineageEdge edge(String parent, String child) {
        return LineageEdge.builder().parentBatchId(parent).childBatchId(child).operation(LineageOperation.DERIVE)
            .organizationId("org1").quantity(BigDecimal.ONE).unit("kg").createdAt(Instant.now()).build();
    }
}
