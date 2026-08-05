package com.agri.supplytracker.shipment.domain;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.math.BigDecimal;
import java.time.Instant;
@Document("custody_transfers")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CustodyTransfer {
    public enum Status { OFFERED, ACCEPTED, REJECTED, COMPLETED }
    @Id private String id;
    @Indexed private String batchId;
    private String senderOrganizationId;
    private String recipientOrganizationId;
    private BigDecimal quantity;
    private String unit;
    private Status status;
    private String offeredBy;
    private String acceptedBy;
    private Instant offeredAt;
    private Instant acceptedAt;
    private Instant completedAt;
}
