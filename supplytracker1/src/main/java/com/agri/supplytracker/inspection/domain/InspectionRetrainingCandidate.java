package com.agri.supplytracker.inspection.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("inspection_retraining_candidates")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InspectionRetrainingCandidate {
    @Id private String id;
    @Indexed private String jobId;
    @Indexed private String organizationId;
    private String objectKey;
    private String inputChecksum;
    private String modelVersion;
    private String datasetVersion;
    private List<String> originalLabels;
    private List<String> correctedLabels;
    private String originalClassification;
    private String correctedClassification;
    private String reason;
    private String reviewer;
    private String status;
    private Instant createdAt;
}
