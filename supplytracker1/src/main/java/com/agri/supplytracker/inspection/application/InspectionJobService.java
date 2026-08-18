package com.agri.supplytracker.inspection.application;

import com.agri.supplytracker.inspection.domain.*;
import com.agri.supplytracker.inspection.persistence.InspectionJobRepository;
import com.agri.supplytracker.inspection.persistence.InspectionQueueMessageRepository;
import com.agri.supplytracker.platform.domain.*;
import com.agri.supplytracker.platform.persistence.*;
import com.agri.supplytracker.platform.security.AuthorizationService;
import com.agri.supplytracker.service.*;
import io.micrometer.core.instrument.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class InspectionJobService {
    private final InspectionJobRepository jobs;
    private final InspectionQueueMessageRepository queue;
    private final IdempotencyRecordRepository idempotency;
    private final OutboxEventRepository outbox;
    private final AuthorizationService authorization;
    private final ObjectStorageService storage;
    private final InspectionInferenceClient inference;
    private final ClassifierService classifier;
    private final NotificationService notifications;
    private final String modelVersion;
    private final String datasetVersion;
    private final double reviewThreshold;
    private final int maxAttempts;
    private final long retryBackoffSeconds;
    private final MeterRegistry meterRegistry;

    public InspectionJobService(InspectionJobRepository jobs, InspectionQueueMessageRepository queue, IdempotencyRecordRepository idempotency,
                                OutboxEventRepository outbox, AuthorizationService authorization,
                                ObjectStorageService storage, InspectionInferenceClient inference,
                                ClassifierService classifier, NotificationService notifications,
                                @Value("${inspection.model.version:local-yolo-placeholder}") String modelVersion,
                                @Value("${inspection.dataset.version:unversioned-local-dev}") String datasetVersion,
                                @Value("${inspection.review.confidence-threshold:0.60}") double reviewThreshold,
                                @Value("${inspection.worker.max-attempts:3}") int maxAttempts,
                                @Value("${inspection.worker.retry-backoff-seconds:30}") long retryBackoffSeconds,
                                MeterRegistry meterRegistry) {
        this.jobs = jobs; this.queue = queue; this.idempotency = idempotency; this.outbox = outbox; this.authorization = authorization;
        this.storage = storage; this.inference = inference; this.classifier = classifier; this.notifications = notifications;
        this.modelVersion = modelVersion; this.datasetVersion = datasetVersion; this.reviewThreshold = reviewThreshold;
        this.maxAttempts = maxAttempts; this.retryBackoffSeconds = retryBackoffSeconds;
        this.meterRegistry = meterRegistry;
    }

    public ObjectStorageService.UploadSlot requestUploadSlot(String organizationId, String filename, String contentType,
                                                             long sizeBytes, String actor) {
        authorization.requireMember(organizationId, actor);
        return storage.createUploadSlot(actor, filename, contentType, sizeBytes);
    }

    @Transactional
    public InspectionJob create(String organizationId, String batchId, String objectKey, String inputChecksum,
                                String contentType, String actor, String key) {
        authorization.requireMember(organizationId, actor);
        requireKey(key);
        if (objectKey == null || objectKey.isBlank()) throw new IllegalArgumentException("objectKey is required");
        if (inputChecksum == null || inputChecksum.isBlank()) throw new IllegalArgumentException("inputChecksum is required");
        if (contentType == null || !contentType.startsWith("image/")) throw new IllegalArgumentException("Inspection input must be an image");
        String requestHash = IdempotencySupport.hash("inspection.create", organizationId, batchId, objectKey, inputChecksum, contentType);
        Optional<IdempotencyRecord> replay = idempotency.findByActorAndKey(actor, key);
        if (replay.isPresent()) {
            IdempotencySupport.requireSameRequest(replay.get(), requestHash);
            return replayJob(replay.get(), actor);
        }
        Instant now = Instant.now();
        InspectionJob job = jobs.save(InspectionJob.builder().organizationId(organizationId).batchId(batchId)
            .requestedBy(actor).status(InspectionJobStatus.QUEUED).objectKey(objectKey).inputChecksum(inputChecksum)
            .contentType(contentType).modelVersion(modelVersion).datasetVersion(datasetVersion)
            .attempts(0).createdAt(now).updatedAt(now).queuedAt(now).nextAttemptAt(now).build());
        queue.save(InspectionQueueMessage.builder().jobId(job.getId()).status(InspectionQueueStatus.READY)
            .attempts(0).createdAt(now).updatedAt(now).nextAttemptAt(now).build());
        outbox.save(OutboxEvent.builder().aggregateType("InspectionJob").aggregateId(job.getId())
            .eventType("INSPECTION_JOB_QUEUED").payload(Map.of("jobId", job.getId(), "organizationId", organizationId))
            .createdAt(now).build());
        idempotency.save(IdempotencyRecord.builder().actor(actor).key(key).requestHash(requestHash)
            .resourceType("INSPECTION_JOB").resourceId(job.getId()).createdAt(now).build());
        meterRegistry.counter("inspection.jobs.queued").increment();
        return job;
    }

    public InspectionJob get(String jobId, String actor) {
        InspectionJob job = jobs.findById(jobId).orElseThrow(() -> new NoSuchElementException("Inspection job not found"));
        authorization.requireMember(job.getOrganizationId(), actor);
        return job;
    }

    public List<InspectionJob> list(String organizationId, String actor) {
        authorization.requireMember(organizationId, actor);
        return jobs.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
    }

    @Transactional
    public Optional<InspectionJob> processNextQueuedJob() {
        Instant pollTime = Instant.now();
        Optional<InspectionQueueMessage> next = queue.findFirstByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            List.of(InspectionQueueStatus.READY, InspectionQueueStatus.RETRY), pollTime);
        if (next.isEmpty()) return Optional.empty();
        InspectionQueueMessage message = next.get();
        InspectionJob job = jobs.findById(message.getJobId()).orElseThrow(() -> new NoSuchElementException("Inspection job not found"));
        Instant started = Instant.now();
        if (job.getQueuedAt() != null) {
            io.micrometer.core.instrument.Timer.builder("inspection.queue.delay")
                .register(meterRegistry)
                .record(java.time.Duration.between(job.getQueuedAt(), started));
        }
        message.setStatus(InspectionQueueStatus.IN_FLIGHT); message.setUpdatedAt(started); queue.save(message);
        job.setStatus(InspectionJobStatus.PROCESSING); job.setProcessingStartedAt(started);
        job.setUpdatedAt(started); job.setAttempts(job.getAttempts() + 1);
        jobs.save(job);
        try {
            ObjectStorageService.StoredObject object = storage.read(job.getObjectKey());
            if (!Objects.equals(job.getInputChecksum(), object.checksum())) throw new IllegalStateException("Inspection input checksum mismatch");
            InspectionInferenceClient.InferenceResult result = inference.analyze(job, object.bytes());
            List<String> labels = result.labels() == null || result.labels().isEmpty() ? List.of("Unknown Product") : result.labels();
            job.setLabels(labels); job.setRawResult(result.rawResult()); job.setConfidence(result.confidence());
            job.setInferenceLatencyMs(result.latencyMs()); job.setClassification(classifier.classifyProduct(labels));
            job.setStatus(result.confidence() < reviewThreshold ? InspectionJobStatus.REVIEW_REQUIRED : InspectionJobStatus.SUCCEEDED);
            job.setCompletedAt(Instant.now()); job.setUpdatedAt(job.getCompletedAt());
            InspectionJob saved = jobs.save(job);
            message.setStatus(InspectionQueueStatus.ACKED); message.setAttempts(job.getAttempts()); message.setUpdatedAt(saved.getCompletedAt());
            queue.save(message);
            outbox.save(OutboxEvent.builder().aggregateType("InspectionJob").aggregateId(saved.getId())
                .eventType("INSPECTION_JOB_COMPLETED").payload(Map.of("jobId", saved.getId(), "status", saved.getStatus().name()))
                .createdAt(Instant.now()).build());
            notifications.notifyUsers(List.of(saved.getRequestedBy()), "INSPECTION_COMPLETED", "Inspection completed",
                "Inspection job " + saved.getId() + " completed with status " + saved.getStatus(), saved.getId());
            meterRegistry.counter("inspection.jobs.completed", "status", saved.getStatus().name()).increment();
            return Optional.of(saved);
        } catch (RuntimeException e) {
            Instant now = Instant.now();
            job.setFailureReason(e.getMessage());
            job.setUpdatedAt(now);
            if (job.getAttempts() < maxAttempts) {
                job.setStatus(InspectionJobStatus.QUEUED);
                job.setQueuedAt(now);
                job.setNextAttemptAt(now.plusSeconds(retryBackoffSeconds * job.getAttempts()));
                InspectionJob saved = jobs.save(job);
                message.setStatus(InspectionQueueStatus.RETRY); message.setAttempts(job.getAttempts());
                message.setLastError(e.getMessage()); message.setNextAttemptAt(saved.getNextAttemptAt()); message.setUpdatedAt(now);
                queue.save(message);
                outbox.save(OutboxEvent.builder().aggregateType("InspectionJob").aggregateId(saved.getId())
                    .eventType("INSPECTION_JOB_RETRY_SCHEDULED").payload(Map.of("jobId", saved.getId(), "attempts", String.valueOf(saved.getAttempts())))
                    .createdAt(now).build());
                meterRegistry.counter("inspection.jobs.retry").increment();
                return Optional.of(saved);
            }
            job.setStatus(InspectionJobStatus.FAILED);
            job.setCompletedAt(now);
            InspectionJob saved = jobs.save(job);
            message.setStatus(InspectionQueueStatus.DLQ); message.setAttempts(job.getAttempts());
            message.setLastError(e.getMessage()); message.setDeadLetteredAt(now); message.setUpdatedAt(now);
            queue.save(message);
            outbox.save(OutboxEvent.builder().aggregateType("InspectionJob").aggregateId(saved.getId())
                .eventType("INSPECTION_JOB_FAILED").payload(Map.of("jobId", saved.getId(), "reason", Objects.toString(e.getMessage(), "")))
                .createdAt(now).build());
            meterRegistry.counter("inspection.jobs.failed").increment();
            return Optional.of(saved);
        }
    }

    private InspectionJob replayJob(IdempotencyRecord record, String actor) {
        if (!"INSPECTION_JOB".equals(record.getResourceType())) throw new IllegalStateException("Idempotency key already used by another command");
        return get(record.getResourceId(), actor);
    }

    private void requireKey(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Idempotency-Key header is required");
    }
}
