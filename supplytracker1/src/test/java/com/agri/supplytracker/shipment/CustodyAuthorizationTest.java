package com.agri.supplytracker.shipment;
import com.agri.supplytracker.catalog.application.BatchService;
import com.agri.supplytracker.organization.persistence.OrganizationRepository;
import com.agri.supplytracker.platform.persistence.IdempotencyRecordRepository;
import com.agri.supplytracker.platform.security.AuthorizationService;
import com.agri.supplytracker.shipment.application.CustodyService;
import com.agri.supplytracker.shipment.domain.CustodyTransfer;
import com.agri.supplytracker.shipment.persistence.CustodyTransferRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.Mockito.*;
class CustodyAuthorizationTest {
    @Test void acceptanceChecksRecipientMembership(){
        CustodyTransferRepository transfers=mock(CustodyTransferRepository.class); BatchService batches=mock(BatchService.class);
        AuthorizationService auth=mock(AuthorizationService.class); OrganizationRepository organizations=mock(OrganizationRepository.class); IdempotencyRecordRepository keys=mock(IdempotencyRecordRepository.class);
        CustodyService service=new CustodyService(transfers,batches,auth,organizations,keys);
        when(keys.findByActorAndKey("bob","k1")).thenReturn(Optional.empty());
        when(transfers.findById("c1")).thenReturn(Optional.of(CustodyTransfer.builder().id("c1").recipientOrganizationId("org-b").status(CustodyTransfer.Status.OFFERED).build()));
        try { service.accept("c1","bob","k1"); } catch (RuntimeException ignored) { }
        verify(auth).requireMember("org-b","bob");
    }
}
