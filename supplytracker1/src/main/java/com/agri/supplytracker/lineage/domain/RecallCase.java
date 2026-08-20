package com.agri.supplytracker.lineage.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("recall_cases")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RecallCase {
    @Id private String id;
    @Indexed private String sourceBatchId;
    @Indexed private String organizationId;
    private String reason;
    private boolean simulation;
    private RecallStatus status;
    private RecallScopeSnapshot scope;
    private RecallTraversalStats traversalStats;
    private List<RecallAction> actions;
    private List<RecallNotice> notices;
    private List<RecallAcknowledgment> acknowledgments;
    private String createdBy;
    private Instant createdAt;
    private String resolvedBy;
    private String resolution;
    private Instant resolvedAt;
}
