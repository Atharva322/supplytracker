package com.agri.supplytracker.lineage.domain;

import lombok.*;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RecallAcknowledgment {
    private String organizationId;
    private String actor;
    private String note;
    private Instant acknowledgedAt;
}
