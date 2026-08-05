package com.agri.supplytracker.traceability.domain;

import com.agri.supplytracker.catalog.domain.BatchStatus;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Set;

@Component
public class BatchTransitionPolicy {
    private static final Map<BatchStatus, Set<BatchStatus>> ALLOWED = Map.of(
        BatchStatus.HARVESTED, Set.of(BatchStatus.PROCESSING, BatchStatus.REJECTED),
        BatchStatus.PROCESSING, Set.of(BatchStatus.QUALITY_APPROVED, BatchStatus.REJECTED),
        BatchStatus.QUALITY_APPROVED, Set.of(BatchStatus.READY_FOR_SHIPMENT, BatchStatus.REJECTED),
        BatchStatus.READY_FOR_SHIPMENT, Set.of(BatchStatus.IN_TRANSIT, BatchStatus.REJECTED),
        BatchStatus.IN_TRANSIT, Set.of(BatchStatus.DELIVERED, BatchStatus.REJECTED)
    );

    public void validate(BatchStatus current, BatchStatus target) {
        if (!ALLOWED.getOrDefault(current, Set.of()).contains(target)) {
            throw new IllegalStateException("Invalid batch transition: " + current + " -> " + target);
        }
    }
}
