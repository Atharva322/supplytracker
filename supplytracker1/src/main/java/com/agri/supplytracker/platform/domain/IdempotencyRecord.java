package com.agri.supplytracker.platform.domain;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document("idempotency_records")
@CompoundIndex(name="actor_key", def="{'actor':1,'key':1}", unique=true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class IdempotencyRecord {
    @Id private String id;
    private String actor;
    private String key;
    private String resourceType;
    private String resourceId;
    private Instant createdAt;
}
