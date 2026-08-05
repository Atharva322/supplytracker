package com.agri.supplytracker.catalog.persistence;
import com.agri.supplytracker.catalog.domain.ProductBatch;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;
public interface ProductBatchRepository extends MongoRepository<ProductBatch, String> {
    Optional<ProductBatch> findByBatchId(String batchId);
    List<ProductBatch> findByOrganizationId(String organizationId);
    List<ProductBatch> findByOrganizationIdOrCustodianOrganizationIdOrPendingCustodianOrganizationId(String organizationId, String custodianOrganizationId, String pendingCustodianOrganizationId);
    Optional<ProductBatch> findByMigrationSourceId(String migrationSourceId);
}
