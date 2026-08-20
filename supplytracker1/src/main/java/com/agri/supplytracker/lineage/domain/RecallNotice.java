package com.agri.supplytracker.lineage.domain;

import lombok.*;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RecallNotice {
    private String organizationId;
    private String recipient;
    private Instant sentAt;
    private boolean simulation;
}
