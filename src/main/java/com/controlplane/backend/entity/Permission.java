package com.controlplane.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

/**
 * A permission is the smallest unit of authorization in the system, for
 * example {@code TASK_CREATE} or {@code EMPLOYEE_DELETE}. Permissions are
 * grouped into {@link Role}s rather than being assigned to users directly,
 * which keeps access control manageable as the platform grows.
 */
@Entity
@Table(
        name = "permissions",
        indexes = {
                @Index(name = "idx_permissions_module", columnList = "module")
        }
)
@lombok.Getter
@lombok.Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "roles")
public class Permission extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    /**
     * Logical module this permission belongs to, e.g. {@code PROJECT},
     * {@code EMPLOYEE}, {@code TASK}. Used to group permissions in the
     * administration UI.
     */
    @Column(name = "module", nullable = false, length = 50)
    private String module;

    @Builder.Default
    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles = new HashSet<>();
}
