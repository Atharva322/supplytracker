package com.agri.supplytracker.inspection;

import com.agri.supplytracker.inspection.application.*;
import com.agri.supplytracker.inspection.domain.*;
import com.agri.supplytracker.inspection.persistence.*;
import com.agri.supplytracker.platform.domain.*;
import com.agri.supplytracker.platform.persistence.*;
import com.agri.supplytracker.platform.security.AuthorizationService;
import com.agri.supplytracker.service.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InspectionJobServiceTest {
    @Test
    void createQueuesJobAndRejectsConflictingReplay() {
        InspectionJobRepository jobs = mock(InspectionJobRepository.class);
        InspectionQueueMessageRepository queue = mock(InspectionQueueMessageRepository.class);
        IdempotencyRecordRepository keys = mock(IdempotencyRecordRepository.class);
        OutboxEventRepository outbox = mock(OutboxEventRepository.class);
        AuthorizationService auth = mock(AuthorizationService.class);
        ObjectStorageService storage = mock(ObjectStorageService.class);
        InspectionInferenceClient inference = mock(InspectionInferenceClient.class);
        ClassifierService classifier = mock(ClassifierService.class);
        NotificationService notifications = mock(NotificationService.class);
        InspectionJobService service = new InspectionJobService(jobs, queue, keys, outbox, auth, storage, inference,
            classifier, notifications, "model-v1", "dataset-v1", 0.60, 3, 30, new SimpleMeterRegistry());

        when(keys.findByActorAndKey("alice", "key-1")).thenReturn(Optional.empty());
        when(jobs.save(any())).thenAnswer(invocation -> {
            InspectionJob job = invocation.getArgument(0);
            job.setId("job-1");
            return job;
        });

        InspectionJob created = service.create("org1", "B-1", "objects/img.jpg", "sha256:abc", "image/jpeg", "alice", "key-1");

        assertEquals("job-1", created.getId());
        assertEquals(InspectionJobStatus.QUEUED, created.getStatus());
        assertEquals("model-v1", created.getModelVersion());
        verify(outbox).save(argThat(event -> "INSPECTION_JOB_QUEUED".equals(event.getEventType())));
        verify(queue).save(argThat(message -> "job-1".equals(message.getJobId()) && message.getStatus() == InspectionQueueStatus.READY));
        verify(keys).save(argThat(record -> "INSPECTION_JOB".equals(record.getResourceType()) && record.getRequestHash() != null));
        verifyNoInteractions(inference);

        when(keys.findByActorAndKey("alice", "key-1")).thenReturn(Optional.of(IdempotencyRecord.builder()
            .actor("alice").key("key-1")
            .requestHash(IdempotencySupport.hash("inspection.create", "org1", "B-1", "objects/img.jpg", "sha256:abc", "image/jpeg"))
            .resourceType("INSPECTION_JOB").resourceId("job-1").build()));
        IllegalStateException conflict = assertThrows(IllegalStateException.class,
            () -> service.create("org1", "B-2", "objects/img.jpg", "sha256:abc", "image/jpeg", "alice", "key-1"));
        assertEquals("Idempotency key already used with a different payload", conflict.getMessage());
    }

    @Test
    void workerMovesLowConfidenceResultToReviewRequiredAndPublishesCompletion() {
        InspectionJobRepository jobs = mock(InspectionJobRepository.class);
        InspectionQueueMessageRepository queue = mock(InspectionQueueMessageRepository.class);
        IdempotencyRecordRepository keys = mock(IdempotencyRecordRepository.class);
        OutboxEventRepository outbox = mock(OutboxEventRepository.class);
        AuthorizationService auth = mock(AuthorizationService.class);
        ObjectStorageService storage = mock(ObjectStorageService.class);
        InspectionInferenceClient inference = mock(InspectionInferenceClient.class);
        ClassifierService classifier = mock(ClassifierService.class);
        NotificationService notifications = mock(NotificationService.class);
        InspectionJobService service = new InspectionJobService(jobs, queue, keys, outbox, auth, storage, inference,
            classifier, notifications, "model-v1", "dataset-v1", 0.60, 3, 30, new SimpleMeterRegistry());
        InspectionJob queued = InspectionJob.builder().id("job-1").organizationId("org1").requestedBy("alice")
            .status(InspectionJobStatus.QUEUED).queuedAt(Instant.now()).attempts(0).objectKey("objects/img.jpg").inputChecksum("sha256:abc").build();
        InspectionQueueMessage message = InspectionQueueMessage.builder().id("msg-1").jobId("job-1").status(InspectionQueueStatus.READY).attempts(0).build();
        when(queue.findFirstByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any(), any())).thenReturn(Optional.of(message));
        when(jobs.findById("job-1")).thenReturn(Optional.of(queued));
        when(jobs.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(storage.read("objects/img.jpg")).thenReturn(new ObjectStorageService.StoredObject("objects/img.jpg", new byte[]{1,2,3}, "image/jpeg", "sha256:abc", 3));
        when(inference.analyze(any(), any())).thenReturn(new InspectionInferenceClient.InferenceResult(
            List.of("mango"), Map.of("source", "test"), 0.42, 25L));
        when(classifier.classifyProduct(List.of("mango"))).thenReturn("Fruits");

        InspectionJob processed = service.processNextQueuedJob().orElseThrow();

        assertEquals(InspectionJobStatus.REVIEW_REQUIRED, processed.getStatus());
        assertEquals("Fruits", processed.getClassification());
        assertEquals(1, processed.getAttempts());
        verify(outbox).save(argThat(event -> "INSPECTION_JOB_COMPLETED".equals(event.getEventType())));
        verify(queue, atLeastOnce()).save(argThat(saved -> saved.getStatus() == InspectionQueueStatus.ACKED));
        verify(notifications).notifyUsers(eq(List.of("alice")), eq("INSPECTION_COMPLETED"), anyString(), anyString(), eq("job-1"));
    }

    @Test
    void workerMarksTerminalFailureWhenInferenceThrows() {
        InspectionJobRepository jobs = mock(InspectionJobRepository.class);
        InspectionQueueMessageRepository queue = mock(InspectionQueueMessageRepository.class);
        IdempotencyRecordRepository keys = mock(IdempotencyRecordRepository.class);
        OutboxEventRepository outbox = mock(OutboxEventRepository.class);
        AuthorizationService auth = mock(AuthorizationService.class);
        ObjectStorageService storage = mock(ObjectStorageService.class);
        InspectionInferenceClient inference = mock(InspectionInferenceClient.class);
        ClassifierService classifier = mock(ClassifierService.class);
        NotificationService notifications = mock(NotificationService.class);
        InspectionJobService service = new InspectionJobService(jobs, queue, keys, outbox, auth, storage, inference,
            classifier, notifications, "model-v1", "dataset-v1", 0.60, 1, 30, new SimpleMeterRegistry());
        InspectionJob queued = InspectionJob.builder().id("job-1").organizationId("org1").requestedBy("alice")
            .status(InspectionJobStatus.QUEUED).queuedAt(Instant.now()).attempts(0).objectKey("objects/img.jpg").inputChecksum("sha256:abc").build();
        InspectionQueueMessage message = InspectionQueueMessage.builder().id("msg-1").jobId("job-1").status(InspectionQueueStatus.READY).attempts(0).build();
        when(queue.findFirstByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any(), any())).thenReturn(Optional.of(message));
        when(jobs.findById("job-1")).thenReturn(Optional.of(queued));
        when(jobs.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(storage.read("objects/img.jpg")).thenReturn(new ObjectStorageService.StoredObject("objects/img.jpg", new byte[]{1,2,3}, "image/jpeg", "sha256:abc", 3));
        when(inference.analyze(any(), any())).thenThrow(new IllegalStateException("service timeout"));

        InspectionJob processed = service.processNextQueuedJob().orElseThrow();

        assertEquals(InspectionJobStatus.FAILED, processed.getStatus());
        assertEquals("service timeout", processed.getFailureReason());
        verify(outbox).save(argThat(event -> "INSPECTION_JOB_FAILED".equals(event.getEventType())));
        verify(queue, atLeastOnce()).save(argThat(saved -> saved.getStatus() == InspectionQueueStatus.DLQ));
    }

    @Test
    void workerRequeuesTransientFailureBeforeMaxAttempts() {
        InspectionJobRepository jobs = mock(InspectionJobRepository.class);
        InspectionQueueMessageRepository queue = mock(InspectionQueueMessageRepository.class);
        IdempotencyRecordRepository keys = mock(IdempotencyRecordRepository.class);
        OutboxEventRepository outbox = mock(OutboxEventRepository.class);
        AuthorizationService auth = mock(AuthorizationService.class);
        ObjectStorageService storage = mock(ObjectStorageService.class);
        InspectionInferenceClient inference = mock(InspectionInferenceClient.class);
        ClassifierService classifier = mock(ClassifierService.class);
        NotificationService notifications = mock(NotificationService.class);
        InspectionJobService service = new InspectionJobService(jobs, queue, keys, outbox, auth, storage, inference,
            classifier, notifications, "model-v1", "dataset-v1", 0.60, 3, 30, new SimpleMeterRegistry());
        InspectionJob queued = InspectionJob.builder().id("job-1").organizationId("org1").requestedBy("alice")
            .status(InspectionJobStatus.QUEUED).queuedAt(Instant.now()).attempts(0).objectKey("objects/img.jpg").inputChecksum("sha256:abc").build();
        InspectionQueueMessage message = InspectionQueueMessage.builder().id("msg-1").jobId("job-1").status(InspectionQueueStatus.READY).attempts(0).build();
        when(queue.findFirstByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any(), any())).thenReturn(Optional.of(message));
        when(jobs.findById("job-1")).thenReturn(Optional.of(queued));
        when(jobs.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(storage.read("objects/img.jpg")).thenReturn(new ObjectStorageService.StoredObject("objects/img.jpg", new byte[]{1,2,3}, "image/jpeg", "sha256:abc", 3));
        when(inference.analyze(any(), any())).thenThrow(new IllegalStateException("temporary timeout"));

        InspectionJob processed = service.processNextQueuedJob().orElseThrow();

        assertEquals(InspectionJobStatus.QUEUED, processed.getStatus());
        assertEquals(1, processed.getAttempts());
        assertNotNull(processed.getNextAttemptAt());
        verify(outbox).save(argThat(event -> "INSPECTION_JOB_RETRY_SCHEDULED".equals(event.getEventType())));
        verify(queue, atLeastOnce()).save(argThat(saved -> saved.getStatus() == InspectionQueueStatus.RETRY));
    }
}
