package com.agri.supplytracker.organization.application;

import com.agri.supplytracker.organization.domain.*;
import com.agri.supplytracker.organization.persistence.*;
import com.agri.supplytracker.platform.security.AuthorizationService;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import com.agri.supplytracker.identity.application.UserDirectoryService;

@Service
public class OrganizationService {
    private final OrganizationRepository organizations;
    private final FacilityRepository facilities;
    private final MembershipRepository memberships;
    private final AuthorizationService authorization;
    private final UserDirectoryService users;

    public OrganizationService(OrganizationRepository organizations, FacilityRepository facilities,
                               MembershipRepository memberships, AuthorizationService authorization, UserDirectoryService users) {
        this.organizations = organizations; this.facilities = facilities;
        this.memberships = memberships; this.authorization = authorization; this.users=users;
    }

    public Organization create(String name, String slug, String actor) {
        if (organizations.existsBySlug(slug)) throw new IllegalArgumentException("Organization slug already exists");
        Organization organization = organizations.save(Organization.builder()
            .name(name).slug(slug.toLowerCase()).createdAt(Instant.now()).build());
        memberships.save(Membership.builder().organizationId(organization.getId()).username(actor)
            .role(Membership.Role.OWNER).build());
        return organization;
    }

    public List<Organization> mine(String actor) {
        List<String> ids = memberships.findByUsername(actor).stream().map(Membership::getOrganizationId).toList();
        return organizations.findAllById(ids);
    }

    public Facility createFacility(String organizationId, String code, String name, String type, String address, String actor) {
        authorization.requireManager(organizationId, actor);
        return facilities.save(Facility.builder().organizationId(organizationId).code(code).name(name)
            .type(type).address(address).build());
    }

    public List<Facility> facilities(String organizationId, String actor) {
        authorization.requireMember(organizationId, actor);
        return facilities.findByOrganizationId(organizationId);
    }

    /** Internal application-level audience lookup; callers never access membership persistence directly. */
    public List<String> memberUsernames(String organizationId) {
        return memberships.findByOrganizationId(organizationId).stream().map(Membership::getUsername).toList();
    }

    public Facility requireFacility(String organizationId, String facilityId, String actor) {
        authorization.requireMember(organizationId, actor);
        Facility facility=facilities.findById(facilityId).orElseThrow(() -> new NoSuchElementException("Facility not found"));
        if(!organizationId.equals(facility.getOrganizationId())) throw new org.springframework.security.access.AccessDeniedException("Facility is outside your organization");
        return facility;
    }

    public Membership addMember(String organizationId,String username,Membership.Role role,String actor) {
        authorization.requireManager(organizationId,actor); users.requireUser(username);
        if(role==Membership.Role.OWNER) throw new IllegalArgumentException("Ownership transfer requires a dedicated workflow");
        return memberships.save(Membership.builder().organizationId(organizationId).username(username).role(role).build());
    }

    public List<Membership> members(String organizationId,String actor) {
        authorization.requireManager(organizationId,actor);
        return memberships.findByOrganizationId(organizationId);
    }
}
