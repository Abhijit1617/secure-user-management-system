package com.controlplane.backend.service.impl;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.organization.CreateOrganizationRequest;
import com.controlplane.backend.dto.organization.OrganizationResponse;
import com.controlplane.backend.dto.organization.OrganizationSettingsRequest;
import com.controlplane.backend.dto.organization.TransferOwnershipRequest;
import com.controlplane.backend.dto.organization.UpdateOrganizationLogoRequest;
import com.controlplane.backend.dto.organization.UpdateOrganizationRequest;
import com.controlplane.backend.dto.organization.UpdateOrganizationStatusRequest;
import com.controlplane.backend.entity.Organization;
import com.controlplane.backend.entity.OrganizationSettings;
import com.controlplane.backend.entity.User;
import com.controlplane.backend.exception.DuplicateResourceException;
import com.controlplane.backend.exception.OrganizationNotEmptyException;
import com.controlplane.backend.exception.ResourceNotFoundException;
import com.controlplane.backend.mapper.OrganizationMapper;
import com.controlplane.backend.repository.DepartmentRepository;
import com.controlplane.backend.repository.OrganizationRepository;
import com.controlplane.backend.repository.UserRepository;
import com.controlplane.backend.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

import static com.controlplane.backend.config.RedisCacheConfig.ORGANIZATION_CACHE;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationServiceImpl.class);

    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final OrganizationMapper organizationMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrganizationResponse> search(String searchTerm, Pageable pageable) {
        Page<Organization> page = StringUtils.hasText(searchTerm)
                ? organizationRepository.findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(
                        searchTerm, searchTerm, pageable)
                : organizationRepository.findAll(pageable);
        return PageResponse.from(page.map(this::toResponseWithDepartmentCount));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = ORGANIZATION_CACHE, key = "#id")
    public OrganizationResponse getById(UUID id) {
        return toResponseWithDepartmentCount(loadOrganization(id));
    }

    @Override
    @Transactional
    public OrganizationResponse create(CreateOrganizationRequest request, UUID ownerId) {
        if (organizationRepository.existsBySlug(request.getSlug())) {
            throw DuplicateResourceException.of("Organization", "slug", request.getSlug());
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", "id", ownerId));

        Organization organization = Organization.builder()
                .name(request.getName())
                .legalName(request.getLegalName())
                .slug(request.getSlug())
                .website(request.getWebsite())
                .industry(request.getIndustry())
                .owner(owner)
                .settings(toSettingsEntity(request.getSettings()))
                .build();

        organization = organizationRepository.save(organization);
        log.info("Created organization [{}] owned by [{}]", organization.getSlug(), owner.getUsername());
        return organizationMapper.toResponse(organization, 0);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = ORGANIZATION_CACHE, key = "#id")
    public OrganizationResponse update(UUID id, UpdateOrganizationRequest request) {
        Organization organization = loadOrganization(id);
        organization.setName(request.getName());
        organization.setLegalName(request.getLegalName());
        organization.setWebsite(request.getWebsite());
        organization.setIndustry(request.getIndustry());
        if (request.getSettings() != null) {
            organization.setSettings(toSettingsEntity(request.getSettings()));
        }
        return toResponseWithDepartmentCount(organizationRepository.save(organization));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = ORGANIZATION_CACHE, key = "#id")
    public OrganizationResponse updateLogo(UUID id, UpdateOrganizationLogoRequest request) {
        Organization organization = loadOrganization(id);
        organization.setLogoUrl(request.getLogoUrl());
        return toResponseWithDepartmentCount(organizationRepository.save(organization));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = ORGANIZATION_CACHE, key = "#id")
    public OrganizationResponse updateStatus(UUID id, UpdateOrganizationStatusRequest request) {
        Organization organization = loadOrganization(id);
        organization.setStatus(request.getStatus());
        log.info("Organization [{}] status changed to {}", organization.getSlug(), request.getStatus());
        return toResponseWithDepartmentCount(organizationRepository.save(organization));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = ORGANIZATION_CACHE, key = "#id")
    public OrganizationResponse transferOwnership(UUID id, TransferOwnershipRequest request) {
        Organization organization = loadOrganization(id);
        User newOwner = userRepository.findById(request.getNewOwnerId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", "id", request.getNewOwnerId()));
        organization.setOwner(newOwner);
        log.info("Organization [{}] ownership transferred to [{}]", organization.getSlug(), newOwner.getUsername());
        return toResponseWithDepartmentCount(organizationRepository.save(organization));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = ORGANIZATION_CACHE, key = "#id")
    public void delete(UUID id) {
        Organization organization = loadOrganization(id);
        long departmentCount = departmentRepository.countByOrganization(organization);
        if (departmentCount > 0) {
            throw new OrganizationNotEmptyException(departmentCount);
        }
        organizationRepository.delete(organization);
        log.info("Deleted organization [{}]", organization.getSlug());
    }

    private Organization loadOrganization(UUID id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Organization", "id", id));
    }

    private OrganizationResponse toResponseWithDepartmentCount(Organization organization) {
        long departmentCount = departmentRepository.countByOrganization(organization);
        return organizationMapper.toResponse(organization, (int) departmentCount);
    }

    private OrganizationSettings toSettingsEntity(OrganizationSettingsRequest request) {
        if (request == null) {
            return OrganizationSettings.builder().build();
        }
        return OrganizationSettings.builder()
                .timezone(request.getTimezone())
                .defaultLocale(request.getDefaultLocale())
                .dateFormat(request.getDateFormat())
                .currency(request.getCurrency())
                .allowSelfRegistration(request.isAllowSelfRegistration())
                .build();
    }
}
