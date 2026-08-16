package com.agri.supplytracker.catalog;
import com.agri.supplytracker.catalog.application.BatchService;
import com.agri.supplytracker.catalog.domain.*;
import com.agri.supplytracker.catalog.persistence.ProductBatchRepository;
import com.agri.supplytracker.organization.domain.Membership;
import com.agri.supplytracker.platform.domain.*;
import com.agri.supplytracker.platform.persistence.*;
import com.agri.supplytracker.platform.security.AuthorizationService;
import com.agri.supplytracker.traceability.domain.BatchTransitionPolicy;
import com.agri.supplytracker.traceability.persistence.TraceabilityEventRepository;
import com.agri.supplytracker.organization.application.OrganizationService;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class BatchIdempotencyTest {
    @Test void transitionReplayReturnsPriorResultWithoutNewWrites(){
        ProductBatchRepository batches=mock(ProductBatchRepository.class); TraceabilityEventRepository events=mock(TraceabilityEventRepository.class);
        OutboxEventRepository outbox=mock(OutboxEventRepository.class); IdempotencyRecordRepository keys=mock(IdempotencyRecordRepository.class);
        AuthorizationService auth=mock(AuthorizationService.class); BatchTransitionPolicy policy=mock(BatchTransitionPolicy.class);
        OrganizationService organizations=mock(OrganizationService.class);
        BatchService service=new BatchService(batches,events,outbox,keys,auth,policy,organizations); ProductBatch prior=ProductBatch.builder().id("db1").batchId("B-1").organizationId("org1").build();
        when(keys.findByActorAndKey("alice","key-1")).thenReturn(Optional.of(IdempotencyRecord.builder().actor("alice").key("key-1")
            .requestHash(IdempotencySupport.hash("batch.transition","B-1",BatchStatus.PROCESSING,0L))
            .resourceType("BATCH").resourceId("db1").build()));
        when(batches.findById("db1")).thenReturn(Optional.of(prior));
        when(auth.isMember("org1","alice")).thenReturn(true);
        assertSame(prior,service.transition("B-1",BatchStatus.PROCESSING,0L,"alice","key-1"));
        verify(batches,never()).save(any()); verify(events,never()).save(any()); verify(outbox,never()).save(any());
    }

    @Test void transitionRejectsSameKeyWithDifferentPayload(){
        ProductBatchRepository batches=mock(ProductBatchRepository.class); TraceabilityEventRepository events=mock(TraceabilityEventRepository.class);
        OutboxEventRepository outbox=mock(OutboxEventRepository.class); IdempotencyRecordRepository keys=mock(IdempotencyRecordRepository.class);
        AuthorizationService auth=mock(AuthorizationService.class); BatchTransitionPolicy policy=mock(BatchTransitionPolicy.class);
        OrganizationService organizations=mock(OrganizationService.class);
        BatchService service=new BatchService(batches,events,outbox,keys,auth,policy,organizations);
        when(keys.findByActorAndKey("alice","key-1")).thenReturn(Optional.of(IdempotencyRecord.builder().actor("alice").key("key-1")
            .requestHash(IdempotencySupport.hash("batch.transition","B-1",BatchStatus.PROCESSING,0L))
            .resourceType("BATCH").resourceId("db1").build()));

        IllegalStateException error=assertThrows(IllegalStateException.class,
            () -> service.transition("B-1",BatchStatus.READY_FOR_SHIPMENT,0L,"alice","key-1"));

        assertEquals("Idempotency key already used with a different payload",error.getMessage());
        verify(batches,never()).save(any()); verify(events,never()).save(any()); verify(outbox,never()).save(any());
    }
}
