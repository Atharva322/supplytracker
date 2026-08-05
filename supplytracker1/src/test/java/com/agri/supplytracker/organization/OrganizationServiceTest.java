package com.agri.supplytracker.organization;
import com.agri.supplytracker.organization.application.OrganizationService;
import com.agri.supplytracker.organization.domain.*;
import com.agri.supplytracker.organization.persistence.*;
import com.agri.supplytracker.platform.security.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.agri.supplytracker.identity.application.UserDirectoryService;
import static org.junit.jupiter.api.Assertions.assertEquals; import static org.mockito.ArgumentMatchers.any; import static org.mockito.Mockito.*;
class OrganizationServiceTest {
    @Test void creatorBecomesOrganizationOwner(){
        OrganizationRepository organizations=mock(OrganizationRepository.class); FacilityRepository facilities=mock(FacilityRepository.class);
        MembershipRepository memberships=mock(MembershipRepository.class); AuthorizationService auth=mock(AuthorizationService.class);
        UserDirectoryService users=mock(UserDirectoryService.class);
        when(organizations.save(any())).thenAnswer(i->{Organization o=i.getArgument(0);o.setId("org1");return o;});
        OrganizationService service=new OrganizationService(organizations,facilities,memberships,auth,users); service.create("Acme Farms","acme-farms","alice");
        ArgumentCaptor<Membership> membership=ArgumentCaptor.forClass(Membership.class); verify(memberships).save(membership.capture());
        assertEquals("org1",membership.getValue().getOrganizationId()); assertEquals("alice",membership.getValue().getUsername()); assertEquals(Membership.Role.OWNER,membership.getValue().getRole());
    }
}
