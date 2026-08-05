package com.agri.supplytracker.catalog.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.*;

@Document("product_batches")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductBatch {
    @Id private String id;
    @Indexed(unique = true) private String batchId;
    @Indexed private String organizationId;
    private String productName;
    private String productType;
    private BigDecimal quantity;
    private String unit;
    private LocalDate harvestDate;
    private String currentFacilityId;
    @Indexed private String custodianOrganizationId;
    @Indexed private String pendingCustodianOrganizationId;
    private String activeCustodyTransferId;
    private BatchStatus status;
    private QualityStatus qualityStatus;
    private Instant createdAt;
    private Instant updatedAt;
    @Indexed(unique = true, sparse = true) private String migrationSourceId;
    @Version private Long version;
}
