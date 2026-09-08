package com.controlplane.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

/**
 * Organizational unit that employees belong to. Departments may be nested,
 * for example {@code Engineering > Platform Engineering > Backend Team},
 * via the self-referential {@link #parentDepartment} association. Every
 * department belongs to exactly one {@link Organization}; {@link #code} is
 * only required to be unique within that organization, not globally.
 */
@Entity
@Table(
        name = "departments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_departments_org_code", columnNames = {"organization_id", "code"})
        },
        indexes = {
                @Index(name = "idx_departments_parent", columnList = "parent_department_id"),
                @Index(name = "idx_departments_organization", columnList = "organization_id")
        }
)
@lombok.Getter
@lombok.Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"employees", "childDepartments", "organization"})
public class Department extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_department_id")
    private Department parentDepartment;

    @Builder.Default
    @OneToMany(mappedBy = "parentDepartment")
    private Set<Department> childDepartments = new HashSet<>();

    /**
     * The employee acting as the head of this department. Nullable because a
     * newly created department may not have a head assigned yet.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "head_employee_id")
    private Employee head;

    @Builder.Default
    @OneToMany(mappedBy = "department")
    private Set<Employee> employees = new HashSet<>();

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
