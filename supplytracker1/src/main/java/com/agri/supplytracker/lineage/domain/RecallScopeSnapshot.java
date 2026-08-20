package com.agri.supplytracker.lineage.domain;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RecallScopeSnapshot {
    private List<String> affectedBatchIds;
    private List<String> affectedShipmentIds;
    private List<String> affectedFacilityIds;
    private List<String> affectedOrganizationIds;
    private List<String> inventoryHolderOrganizationIds;
    private List<String> recipientOrganizationIds;
    private Map<String, String> explanations;
    private Instant capturedAt;
}
