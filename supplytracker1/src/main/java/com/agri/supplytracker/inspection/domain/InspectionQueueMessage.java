package com.agri.supplytracker.inspection.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("inspection_queue_messages")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InspectionQueueMessage {
    @Id private String id;
    @Indexed private String jobId;
    @Indexed private InspectionQueueStatus status;
    @Indexed private Instant nextAttemptAt;
    private int attempts;
    private String lastError;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deadLetteredAt;
}
