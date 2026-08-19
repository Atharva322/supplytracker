package com.agri.supplytracker.inspection.application;

import com.agri.supplytracker.inspection.domain.*;
import com.agri.supplytracker.inspection.persistence.*;
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
    private final InspectionReviewActionRepository reviews;
    private final InspectionRetrainingCandidateRepository retrainingCandidates;
    private final IdempotencyRecordRepository idempotency;
    private final OutboxEventRepository outbox;
    private final AuthorizationService authorization;
    private final ObjectStorageService storage;
    private final InspectionInferenceClient inference;
    private final InspectionScoringService scoring;
    private final ClassifierService classifier;
    private final NotificationService notifications;
    private final String modelVersion;
    private final String datasetVersion;
    private final String preprocessingVersion;
    private final String labelMapVersion;
    private final int maxAttempts;
    private final long retryBackoffSeconds;
    private final MeterRegistry meterRegistry;

    public InspectionJobService(InspectionJobRepository jobs, InspectionQueueMessageRepository queue,
                                InspectionReviewActionRepository reviews, InspectionRetrainingCandidateRepository retrainingCandidates,
                                IdempotencyRecordRepository idempotency,
                                OutboxEventRepository outbox, AuthorizationService authorization,
                                ObjectStorageService storage, InspectionInferenceClient inference, InspectionScoringService scoring,
                                ClassifierService classifier, NotificationService notifications,
                                @Value("${inspection.model.version:local-yolo-placeholder}") String modelVersion,
                                @Value("${inspection.dataset.version:unversioned-local-dev}") String datasetVersion,
                                @Value("${inspection.preprocessing.version:preprocess-local-v1}") String preprocessingVersion,
                                @Value("${inspection.label-map.version:coco-local-v1}") String labelMapVersion,
                                @Value("${inspection.worker.max-attempts:3}") int maxAttempts,
                                @Value("${inspection.worker.retry-backoff-seconds:30}") long retryBackoffSeconds,
                                MeterRegistry meterRegistry) {
        this.jobs = jobs; this.queue = queue; this.reviews = reviews; this.retrainingCandidates = retrainingCandidates;
        this.idempotency = idempotency; this.outbox = outbox; this.authorization = authorization;
        this.storage = storage; this.inference = inference; this.scoring = scoring; this.classifier = classifier; this.notifications = notifications;
        this.modelVersion = modelVersion; this.datasetVersion = datasetVersion; this.preprocessingVersion = preprocessingVersion; this.labelMapVersion = labelMapVersion;
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
            .preprocessingVersion(preprocessingVersion).labelMapVersion(labelMapVersion)
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
            InspectionScoringService.Score score = scoring.score(job.getClassification(), labels, result.confidence());
            job.setQualityScore(score.qualityScore()); job.setQualityBand(score.qualityBand());
            job.setReviewConfidenceThreshold(score.reviewThreshold()); job.setPolicySensitive(score.policySensitive());
            job.setScoringProfileVersion(score.profileVersion()); job.setThresholdVersion(score.thresholdVersion());
            job.setAutomatedDecision(score.decision()); job.setFinalDecision(score.decision());
            job.setStatus(score.decision() == InspectionDecision.REVIEW ? InspectionJobStatus.REVIEW_REQUIRED : InspectionJobStatus.SUCCEEDED);
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

    @Transactional
    public InspectionJob review(String jobId, InspectionReviewActionType action, List<String> correctedLabels,
                                String correctedClassification, String reason, String reviewer) {
        InspectionJob job = jobs.findById(jobId).orElseThrow(() -> new NoSuchElementException("Inspection job not found"));
        authorization.requireManager(job.getOrganizationId(), reviewer);
        if (job.getStatus() == InspectionJobStatus.QUEUED || job.getStatus() == InspectionJobStatus.PROCESSING) {
            throw new IllegalStateException("Inspection job is not ready for review");
        }
        if ((action == InspectionReviewActionType.CORRECT || action == InspectionReviewActionType.REJECT) && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("reason is required for correction or rejection");
        }
        List<String> previousLabels = copy(job.getLabels());
        String previousClassification = job.getClassification();
        InspectionDecision previousDecision = job.getFinalDecision() == null ? job.getAutomatedDecision() : job.getFinalDecision();
        Instant now = Instant.now();
        InspectionDecision finalDecision = action == InspectionReviewActionType.REJECT ? InspectionDecision.REJECT : InspectionDecision.APPROVE;
        List<String> finalLabels = action == InspectionReviewActionType.CORRECT ? requireLabels(correctedLabels) : previousLabels;
        String finalClassification = action == InspectionReviewActionType.CORRECT
            ? classifyCorrection(finalLabels, correctedClassification) : previousClassification;
        InspectionReviewAction review = reviews.save(InspectionReviewAction.builder()
            .jobId(job.getId()).organizationId(job.getOrganizationId()).action(action)
            .previousDecision(previousDecision).finalDecision(finalDecision)
            .previousLabels(previousLabels).correctedLabels(finalLabels)
            .previousClassification(previousClassification).correctedClassification(finalClassification)
            .reason(reason).reviewer(reviewer).modelVersion(job.getModelVersion()).datasetVersion(job.getDatasetVersion())
            .thresholdVersion(job.getThresholdVersion()).scoringProfileVersion(job.getScoringProfileVersion())
            .createdAt(now).build());
        job.setLabels(finalLabels); job.setClassification(finalClassification); job.setFinalDecision(finalDecision);
        job.setOverrideReason(reason); job.setReviewedBy(reviewer); job.setReviewedAt(now); job.setReviewActionId(review.getId());
        job.setStatus(InspectionJobStatus.REVIEWED); job.setUpdatedAt(now);
        InspectionJob saved = jobs.save(job);
        if (action == InspectionReviewActionType.CORRECT || action == InspectionReviewActionType.REJECT) {
            retrainingCandidates.save(InspectionRetrainingCandidate.builder()
                .jobId(job.getId()).organizationId(job.getOrganizationId()).objectKey(job.getObjectKey()).inputChecksum(job.getInputChecksum())
                .modelVersion(job.getModelVersion()).datasetVersion(job.getDatasetVersion())
                .originalLabels(previousLabels).correctedLabels(finalLabels)
                .originalClassification(previousClassification).correctedClassification(finalClassification)
                .reason(reason).reviewer(reviewer).status("QUEUED").createdAt(now).build());
        }
        outbox.save(OutboxEvent.builder().aggregateType("InspectionJob").aggregateId(saved.getId())
            .eventType("INSPECTION_REVIEW_RECORDED")
            .payload(Map.of("jobId", saved.getId(), "action", action.name(), "finalDecision", finalDecision.name()))
            .createdAt(now).build());
        meterRegistry.counter("inspection.reviews.recorded", "action", action.name()).increment();
        return saved;
    }

    public List<InspectionReviewAction> reviews(String jobId, String actor) {
        InspectionJob job = get(jobId, actor);
        return reviews.findByJobIdOrderByCreatedAtAsc(job.getId());
    }

    private InspectionJob replayJob(IdempotencyRecord record, String actor) {
        if (!"INSPECTION_JOB".equals(record.getResourceType())) throw new IllegalStateException("Idempotency key already used by another command");
        return get(record.getResourceId(), actor);
    }

    private void requireKey(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Idempotency-Key header is required");
    }

    private List<String> copy(List<String> labels) {
        return labels == null ? List.of() : List.copyOf(labels);
    }

    private List<String> requireLabels(List<String> labels) {
        if (labels == null || labels.isEmpty() || labels.stream().anyMatch(label -> label == null || label.isBlank())) {
            throw new IllegalArgumentException("correctedLabels are required");
        }
        return List.copyOf(labels);
    }

    private String classifyCorrection(List<String> labels, String correctedClassification) {
        if (correctedClassification != null && !correctedClassification.isBlank()) return correctedClassification;
        return classifier.classifyProduct(labels);
    }
}
