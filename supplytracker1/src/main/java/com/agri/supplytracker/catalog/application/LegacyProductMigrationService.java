package com.agri.supplytracker.catalog.application;

import com.agri.supplytracker.catalog.domain.*;
import com.agri.supplytracker.catalog.persistence.ProductBatchRepository;
import com.agri.supplytracker.model.*;
import com.agri.supplytracker.platform.security.AuthorizationService;
import com.agri.supplytracker.repository.ProductRepository;
import com.agri.supplytracker.traceability.domain.TraceEventType;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class LegacyProductMigrationService {
    private final ProductRepository legacy;
    private final ProductBatchRepository batches;
    private final BatchService batchService;
    private final AuthorizationService authorization;
    public LegacyProductMigrationService(ProductRepository legacy, ProductBatchRepository batches, BatchService batchService, AuthorizationService authorization) {
        this.legacy=legacy; this.batches=batches; this.batchService=batchService; this.authorization=authorization;
    }
    public record MigrationReport(boolean dryRun,int scanned,int migrated,int skipped,List<String> failures) {}

    public MigrationReport migrate(String organizationId, BigDecimal defaultQuantity, String defaultUnit, boolean dryRun, String actor) {
        authorization.requireManager(organizationId,actor);
        if(defaultQuantity==null||defaultQuantity.signum()<=0) throw new IllegalArgumentException("Default quantity must be positive");
        List<Product> source=legacy.findAll(); int migrated=0,skipped=0; List<String> failures=new ArrayList<>();
        for(Product product:source) {
            if(product.getId()!=null && batches.findByMigrationSourceId(product.getId()).isPresent()){skipped++;continue;}
            try {
                LocalDate harvest=LocalDate.parse(product.getHarvestDate());
                if(dryRun){migrated++;continue;}
                ProductBatch batch=ProductBatch.builder().batchId(product.getBatchId()).organizationId(organizationId)
                    .productName(product.getName()).productType(product.getType()).quantity(defaultQuantity).unit(defaultUnit).harvestDate(harvest)
                    .custodianOrganizationId(organizationId).status(mapStatus(product.getStatus())).qualityStatus(mapQuality(product.getStatus()))
                    .createdAt(Instant.now()).updatedAt(Instant.now()).migrationSourceId(product.getId()).build();
                List<Map<String,String>> legacyStages=new ArrayList<>();
                if(product.getTrackingHistory()!=null) for(TrackingStage stage:product.getTrackingHistory()) {
                    Map<String,String> metadata=new LinkedHashMap<>(); metadata.put("legacyStage",String.valueOf(stage.getStage()));
                    metadata.put("location",String.valueOf(stage.getLocation())); metadata.put("timestamp",String.valueOf(stage.getTimestamp()));
                    legacyStages.add(metadata);
                }
                batchService.importLegacy(batch,actor,legacyStages);
                migrated++;
            } catch(Exception error) { failures.add(product.getId()+": "+error.getMessage()); }
        }
        return new MigrationReport(dryRun,source.size(),migrated,skipped,List.copyOf(failures));
    }

    private BatchStatus mapStatus(String status) {
        if(status==null) return BatchStatus.HARVESTED;
        String normalized=status.trim().toUpperCase().replace(' ','_');
        return switch(normalized) {
            case "PROCESSING" -> BatchStatus.PROCESSING;
            case "QUALITY_APPROVED","QUALITY_CHECK" -> BatchStatus.QUALITY_APPROVED;
            case "READY_FOR_SHIPMENT","IN_WAREHOUSE","WAREHOUSE" -> BatchStatus.READY_FOR_SHIPMENT;
            case "IN_TRANSIT","DISTRIBUTION" -> BatchStatus.IN_TRANSIT;
            case "DELIVERED","RETAIL" -> BatchStatus.DELIVERED;
            case "REJECTED" -> BatchStatus.REJECTED;
            default -> BatchStatus.HARVESTED;
        };
    }

    private QualityStatus mapQuality(String status) {
        if(status==null) return QualityStatus.PENDING;
        String normalized=status.trim().toUpperCase().replace(' ','_');
        if(normalized.equals("REJECTED")) return QualityStatus.REJECTED;
        return switch(normalized) {
            case "QUALITY_APPROVED","QUALITY_CHECK","READY_FOR_SHIPMENT","IN_WAREHOUSE","WAREHOUSE","IN_TRANSIT","DISTRIBUTION","DELIVERED","RETAIL" -> QualityStatus.APPROVED;
            default -> QualityStatus.PENDING;
        };
    }
}
