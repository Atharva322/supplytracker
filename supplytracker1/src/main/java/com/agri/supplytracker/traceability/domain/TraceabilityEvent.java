package com.agri.supplytracker.traceability.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.Map;

@Document("trace_events")
@CompoundIndex(name = "batch_sequence", def = "{'batchId':1,'sequenceNumber':1}", unique = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TraceabilityEvent {
    @Id private String id;
    private String batchId;
    private long sequenceNumber;
    private TraceEventType type;
    private String organizationId;
    private String actor;
    private Instant occurredAt;
    private Map<String, String> metadata;
}
