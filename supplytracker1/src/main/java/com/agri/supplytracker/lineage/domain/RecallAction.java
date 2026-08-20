package com.agri.supplytracker.lineage.domain;

import lombok.*;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RecallAction {
    private String action;
    private String actor;
    private String note;
    private Instant createdAt;
}
