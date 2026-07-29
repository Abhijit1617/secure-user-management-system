package com.controlplane.backend.controller;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.organization.CreateOrganizationRequest;
import com.controlplane.backend.dto.organization.OrganizationResponse;
import com.controlplane.backend.dto.organization.TransferOwnershipRequest;
import com.controlplane.backend.dto.organization.UpdateOrganizationLogoRequest;
import com.controlplane.backend.dto.organization.UpdateOrganizationRequest;
import com.controlplane.backend.dto.organization.UpdateOrganizationStatusRequest;
import com.controlplane.backend.security.CustomUserDetails;
import com.controlplane.backend.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Organization Management", description = "Tenant-level organization CRUD, settings, logo, status and ownership")
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @PreAuthorize("hasAuthority('ORGANIZATION_READ')")
    @Operation(summary = "Search organizations with pagination")
    public ResponseEntity<PageResponse<OrganizationResponse>> search(
            @RequestParam(required = false) String searchTerm,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(organizationService.search(searchTerm, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORGANIZATION_READ')")
    @Operation(summary = "Get an organization by id, including its settings and department count")
    public ResponseEntity<OrganizationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(organizationService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ORGANIZATION_CREATE')")
    @Operation(summary = "Create a new organization",
            description = "The authenticated user becomes the organization's owner.")
    public ResponseEntity<OrganizationResponse> create(@AuthenticationPrincipal CustomUserDetails principal,
                                                        @Valid @RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.create(request, principal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE')")
    @Operation(summary = "Update an organization's details and settings")
    public ResponseEntity<OrganizationResponse> update(@PathVariable UUID id,
                                                        @Valid @RequestBody UpdateOrganizationRequest request) {
        return ResponseEntity.ok(organizationService.update(id, request));
    }

    @PutMapping("/{id}/logo")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE')")
    @Operation(summary = "Update an organization's logo URL")
    public ResponseEntity<OrganizationResponse> updateLogo(@PathVariable UUID id,
                                                            @Valid @RequestBody UpdateOrganizationLogoRequest request) {
        return ResponseEntity.ok(organizationService.updateLogo(id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE')")
    @Operation(summary = "Change an organization's status (active, suspended, archived)")
    public ResponseEntity<OrganizationResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateOrganizationStatusRequest request) {
        return ResponseEntity.ok(organizationService.updateStatus(id, request));
    }

    @PutMapping("/{id}/ownership")
    @PreAuthorize("hasAuthority('ORGANIZATION_MANAGE_OWNERSHIP')")
    @Operation(summary = "Transfer organization ownership to another user")
    public ResponseEntity<OrganizationResponse> transferOwnership(
            @PathVariable UUID id, @Valid @RequestBody TransferOwnershipRequest request) {
        return ResponseEntity.ok(organizationService.transferOwnership(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ORGANIZATION_DELETE')")
    @Operation(summary = "Delete an organization",
            description = "Fails with 409 Conflict if the organization still has departments.")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        organizationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
