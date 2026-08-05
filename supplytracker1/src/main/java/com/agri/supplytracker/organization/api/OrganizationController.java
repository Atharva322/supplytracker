package com.agri.supplytracker.organization.api;

import com.agri.supplytracker.organization.application.OrganizationService;
import com.agri.supplytracker.organization.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v2/organizations")
public class OrganizationController {
    private final OrganizationService service;
    public OrganizationController(OrganizationService service) { this.service = service; }

    public record CreateOrganizationRequest(@NotBlank String name, @NotBlank @Pattern(regexp="[a-z0-9-]{3,50}") String slug) {}
    public record CreateFacilityRequest(@NotBlank String code, @NotBlank String name, @NotBlank String type, String address) {}
    public record AddMemberRequest(@NotBlank String username, @NotNull Membership.Role role) {}

    @PostMapping
    public ResponseEntity<Organization> create(@Valid @RequestBody CreateOrganizationRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request.name(), request.slug(), auth.getName()));
    }
    @GetMapping public List<Organization> mine(Authentication auth) { return service.mine(auth.getName()); }
    @PostMapping("/{organizationId}/facilities")
    public ResponseEntity<Facility> createFacility(@PathVariable String organizationId, @Valid @RequestBody CreateFacilityRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createFacility(organizationId, request.code(), request.name(), request.type(), request.address(), auth.getName()));
    }
    @GetMapping("/{organizationId}/facilities")
    public List<Facility> facilities(@PathVariable String organizationId, Authentication auth) { return service.facilities(organizationId, auth.getName()); }
    @PostMapping("/{organizationId}/members")
    public ResponseEntity<Membership> addMember(@PathVariable String organizationId,@Valid @RequestBody AddMemberRequest request,Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addMember(organizationId,request.username(),request.role(),auth.getName()));
    }
    @GetMapping("/{organizationId}/members")
    public List<Membership> members(@PathVariable String organizationId,Authentication auth){return service.members(organizationId,auth.getName());}
}
