package com.agri.supplytracker.lineage.domain;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RecallTraversalStats {
    private int nodesVisited;
    private int edgesVisited;
    private int maxDepthReached;
    private boolean truncated;
    private String truncationReason;
}
