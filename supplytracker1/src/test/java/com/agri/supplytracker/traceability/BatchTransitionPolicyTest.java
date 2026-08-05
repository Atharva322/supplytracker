package com.agri.supplytracker.traceability;
import com.agri.supplytracker.catalog.domain.BatchStatus;
import com.agri.supplytracker.traceability.domain.BatchTransitionPolicy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BatchTransitionPolicyTest {
    private final BatchTransitionPolicy policy=new BatchTransitionPolicy();
    @Test void allowsExpectedHappyPath(){assertDoesNotThrow(()->policy.validate(BatchStatus.HARVESTED,BatchStatus.PROCESSING));assertDoesNotThrow(()->policy.validate(BatchStatus.IN_TRANSIT,BatchStatus.DELIVERED));}
    @Test void blocksSkippedAndBackwardTransitions(){assertThrows(IllegalStateException.class,()->policy.validate(BatchStatus.HARVESTED,BatchStatus.DELIVERED));assertThrows(IllegalStateException.class,()->policy.validate(BatchStatus.DELIVERED,BatchStatus.IN_TRANSIT));}
}
