package com.agri.supplytracker.inspection.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document("inspection_jobs")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InspectionJob {
    @Id private String id;
    @Indexed private String organizationId;
    @Indexed private String batchId;
    @Indexed private String requestedBy;
    @Indexed private InspectionJobStatus status;
    private String objectKey;
    private String inputChecksum;
    private String contentType;
    private String modelVersion;
    private String datasetVersion;
    private String preprocessingVersion;
    private String labelMapVersion;
    private String thresholdVersion;
    private String scoringProfileVersion;
    private Double reviewConfidenceThreshold;
    private List<String> labels;
    private String classification;
    private Double confidence;
    private Long inferenceLatencyMs;
    private Map<String, String> rawResult;
    private InspectionDecision automatedDecision;
    private InspectionDecision finalDecision;
    private Integer qualityScore;
    private String qualityBand;
    private Boolean policySensitive;
    private String overrideReason;
    private String reviewedBy;
    private Instant reviewedAt;
    private String reviewActionId;
    private String failureReason;
    private int attempts;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant queuedAt;
    private Instant nextAttemptAt;
    private Instant processingStartedAt;
    private Instant completedAt;
}
