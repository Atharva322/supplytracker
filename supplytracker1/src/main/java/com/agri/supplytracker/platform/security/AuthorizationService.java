package com.agri.supplytracker.platform.security;

import com.agri.supplytracker.organization.domain.Membership;
import com.agri.supplytracker.organization.persistence.MembershipRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {
    private final MembershipRepository memberships;
    public AuthorizationService(MembershipRepository memberships) { this.memberships = memberships; }

    public Membership requireMember(String organizationId, String username) {
        return memberships.findByOrganizationIdAndUsername(organizationId, username)
            .orElseThrow(() -> new AccessDeniedException("Not a member of this organization"));
    }

    public Membership requireManager(String organizationId, String username) {
        Membership membership = requireMember(organizationId, username);
        if (membership.getRole() != Membership.Role.OWNER && membership.getRole() != Membership.Role.ADMIN) {
            throw new AccessDeniedException("Organization administrator permission required");
        }
        return membership;
    }

    public boolean isMember(String organizationId, String username) {
        return organizationId != null && memberships.findByOrganizationIdAndUsername(organizationId, username).isPresent();
    }
}
