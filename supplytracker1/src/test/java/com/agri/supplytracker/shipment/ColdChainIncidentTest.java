package com.agri.supplytracker.shipment;
import com.agri.supplytracker.catalog.application.BatchService;
import com.agri.supplytracker.catalog.domain.ProductBatch;
import com.agri.supplytracker.platform.persistence.IdempotencyRecordRepository;
import com.agri.supplytracker.platform.security.AuthorizationService;
import com.agri.supplytracker.organization.application.OrganizationService;
import com.agri.supplytracker.service.NotificationService;
import com.agri.supplytracker.shipment.application.*;
import com.agri.supplytracker.shipment.domain.*;
import com.agri.supplytracker.shipment.persistence.*;
import com.agri.supplytracker.traceability.domain.TraceEventType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal; import java.time.Instant; import java.util.*;
import static org.mockito.ArgumentMatchers.*; import static org.mockito.Mockito.*;
class ColdChainIncidentTest {
    @Test void outOfRangeReadingCreatesIncidentAndTraceEvent(){
        ShipmentRepository shipments=mock(ShipmentRepository.class); SensorReadingRepository readings=mock(SensorReadingRepository.class);
        ColdChainIncidentRepository incidents=mock(ColdChainIncidentRepository.class); CustodyTransferRepository transfers=mock(CustodyTransferRepository.class);
        CustodyService custody=mock(CustodyService.class); BatchService batches=mock(BatchService.class); AuthorizationService auth=mock(AuthorizationService.class);
        IdempotencyRecordRepository keys=mock(IdempotencyRecordRepository.class);
        OrganizationService organizations=mock(OrganizationService.class); NotificationService notifications=mock(NotificationService.class);
        ShipmentService service=new ShipmentService(shipments,readings,incidents,transfers,custody,batches,auth,keys,organizations,notifications);
        Shipment shipment=Shipment.builder().id("s1").custodyTransferId("c1").senderOrganizationId("o1").recipientOrganizationId("o2")
            .status(Shipment.Status.IN_TRANSIT).minimumTemperatureC(new BigDecimal("2")).maximumTemperatureC(new BigDecimal("8"))
            .lines(List.of(ShipmentLine.builder().batchId("B1").quantity(BigDecimal.ONE).unit("kg").build())).build();
        when(shipments.findById("s1")).thenReturn(Optional.of(shipment)); when(auth.isMember("o1","alice")).thenReturn(true);
        when(readings.findByReadingId("r1")).thenReturn(Optional.empty()); when(readings.save(any())).thenAnswer(i->i.getArgument(0));
        when(custody.get("c1")).thenReturn(CustodyTransfer.builder().offeredBy("sender").build());
        when(organizations.memberUsernames("o1")).thenReturn(List.of("alice")); when(organizations.memberUsernames("o2")).thenReturn(List.of("bob"));
        when(batches.get("B1","sender")).thenReturn(ProductBatch.builder().batchId("B1").organizationId("o1").build());
        service.ingest("s1","r1","sensor-1",new BigDecimal("11"),new BigDecimal("70"),Instant.now(),"alice");
        verify(incidents).save(any(ColdChainIncident.class)); verify(batches).appendEvent(any(),eq(TraceEventType.COLD_CHAIN_EXCURSION),eq("alice"),anyMap());
        verify(notifications).notifyUsers(anyList(),eq("COLD_CHAIN_EXCURSION"),anyString(),anyString(),eq("s1"));
    }
}
