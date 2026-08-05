package com.agri.supplytracker.platform.domain;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.Map;
@Document("outbox_events")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OutboxEvent {
    @Id private String id;
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private Map<String, String> payload;
    private Instant createdAt;
    private Instant publishedAt;
}
