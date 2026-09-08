package com.controlplane.backend.service;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.organization.CreateOrganizationRequest;
import com.controlplane.backend.dto.organization.OrganizationResponse;
import com.controlplane.backend.dto.organization.TransferOwnershipRequest;
import com.controlplane.backend.dto.organization.UpdateOrganizationLogoRequest;
import com.controlplane.backend.dto.organization.UpdateOrganizationRequest;
import com.controlplane.backend.dto.organization.UpdateOrganizationStatusRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrganizationService {

    PageResponse<OrganizationResponse> search(String searchTerm, Pageable pageable);

    OrganizationResponse getById(UUID id);

    OrganizationResponse create(CreateOrganizationRequest request, UUID ownerId);

    OrganizationResponse update(UUID id, UpdateOrganizationRequest request);

    OrganizationResponse updateLogo(UUID id, UpdateOrganizationLogoRequest request);

    OrganizationResponse updateStatus(UUID id, UpdateOrganizationStatusRequest request);

    OrganizationResponse transferOwnership(UUID id, TransferOwnershipRequest request);

    void delete(UUID id);
}
