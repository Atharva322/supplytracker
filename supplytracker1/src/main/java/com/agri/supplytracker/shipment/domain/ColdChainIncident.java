package com.agri.supplytracker.shipment.domain;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.math.BigDecimal;
import java.time.Instant;
@Document("cold_chain_incidents")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ColdChainIncident {
    @Id private String id;
    @Indexed private String shipmentId;
    private String readingId;
    private String batchId;
    private BigDecimal temperatureC;
    private BigDecimal allowedMinimumC;
    private BigDecimal allowedMaximumC;
    private Instant detectedAt;
    private String status;
}
