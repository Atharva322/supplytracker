package com.agri.supplytracker.inspection.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("inspection_review_actions")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InspectionReviewAction {
    @Id private String id;
    @Indexed private String jobId;
    @Indexed private String organizationId;
    private InspectionReviewActionType action;
    private InspectionDecision previousDecision;
    private InspectionDecision finalDecision;
    private List<String> previousLabels;
    private List<String> correctedLabels;
    private String previousClassification;
    private String correctedClassification;
    private String reason;
    @Indexed private String reviewer;
    private String modelVersion;
    private String datasetVersion;
    private String thresholdVersion;
    private String scoringProfileVersion;
    private Instant createdAt;
}
