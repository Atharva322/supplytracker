package com.agri.supplytracker.catalog;
import com.agri.supplytracker.catalog.application.*;
import com.agri.supplytracker.catalog.persistence.ProductBatchRepository;
import com.agri.supplytracker.model.Product;
import com.agri.supplytracker.platform.security.AuthorizationService;
import com.agri.supplytracker.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal; import java.util.*;
import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;
class LegacyMigrationDryRunTest {
    @Test void dryRunReportsInvalidLegacyDatesWithoutWriting(){
        ProductRepository legacy=mock(ProductRepository.class); ProductBatchRepository batches=mock(ProductBatchRepository.class);
        BatchService batchService=mock(BatchService.class); AuthorizationService auth=mock(AuthorizationService.class);
        LegacyProductMigrationService service=new LegacyProductMigrationService(legacy,batches,batchService,auth);
        Product good=Product.builder().id("p1").batchId("B1").name("Apple").type("Fruit").harvestDate("2026-08-05").build();
        Product bad=Product.builder().id("p2").batchId("B2").name("Pear").type("Fruit").harvestDate("not-a-date").build();
        when(legacy.findAll()).thenReturn(List.of(good,bad)); when(batches.findByMigrationSourceId(anyString())).thenReturn(Optional.empty());
        var report=service.migrate("org1",BigDecimal.ONE,"kg",true,"alice");
        assertEquals(2,report.scanned()); assertEquals(1,report.migrated()); assertEquals(1,report.failures().size());
        verify(batchService,never()).importLegacy(any(),anyString(),anyList());
    }
}
