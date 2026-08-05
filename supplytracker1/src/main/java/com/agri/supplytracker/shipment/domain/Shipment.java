package com.agri.supplytracker.shipment.domain;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
@Document("shipments")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Shipment {
    public enum Status { DRAFT, DISPATCHED, IN_TRANSIT, DELIVERED }
    @Id private String id;
    private String custodyTransferId;
    @Indexed private String senderOrganizationId;
    @Indexed private String recipientOrganizationId;
    private List<ShipmentLine> lines;
    private BigDecimal minimumTemperatureC;
    private BigDecimal maximumTemperatureC;
    private Status status;
    private Instant createdAt;
    private Instant dispatchedAt;
    private Instant receivedAt;
    @Version private Long version;
}
