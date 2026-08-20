package com.agri.supplytracker.lineage.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Document("lineage_edges")
@CompoundIndex(name = "parent_child_operation", def = "{'parentBatchId':1,'childBatchId':1,'operation':1}", unique = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LineageEdge {
    @Id private String id;
    @Indexed private String parentBatchId;
    @Indexed private String childBatchId;
    private LineageOperation operation;
    @Indexed private String organizationId;
    private BigDecimal quantity;
    private String unit;
    private String actor;
    private Instant createdAt;
    private Map<String, String> metadata;
}
