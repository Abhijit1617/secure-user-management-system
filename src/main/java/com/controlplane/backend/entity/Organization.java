package com.controlplane.backend.entity;

import com.controlplane.backend.entity.enums.OrganizationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

/**
 * The top-level tenant boundary. Every {@link Department} (and, through it,
 * every employee, project and task) belongs to exactly one organization.
 * {@link #owner} is the account with ultimate administrative authority over
 * the tenant, distinct from day-to-day {@code SUPER_ADMIN}/{@code ADMIN}
 * role holders who manage it operationally.
 */
@Entity
@Table(
        name = "organizations",
        indexes = {
                @Index(name = "idx_organizations_slug", columnList = "slug", unique = true),
                @Index(name = "idx_organizations_status", columnList = "status"),
                @Index(name = "idx_organizations_owner", columnList = "owner_id")
        }
)
@lombok.Getter
@lombok.Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"owner", "departments"})
public class Organization extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "legal_name", length = 200)
    private String legalName;

    /**
     * URL-safe unique identifier used in tenant-scoped links, e.g.
     * {@code acme-corp} in {@code https://app.example.com/acme-corp}.
     */
    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Embedded
    @Builder.Default
    private OrganizationSettings settings = new OrganizationSettings();

    @Builder.Default
    @OneToMany(mappedBy = "organization")
    private Set<Department> departments = new HashSet<>();
}
