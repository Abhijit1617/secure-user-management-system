package com.controlplane.backend.repository.spec;

import com.controlplane.backend.entity.Organization;
import com.controlplane.backend.entity.Project;
import com.controlplane.backend.entity.enums.Priority;
import com.controlplane.backend.entity.enums.ProjectStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.UUID;

public final class ProjectSpecifications {

    private ProjectSpecifications() {
    }

    public static Specification<Project> belongsToOrganization(Organization organization) {
        return (root, query, builder) -> builder.equal(root.get("department").get("organization"), organization);
    }

    public static Specification<Project> hasDepartment(UUID departmentId) {
        if (departmentId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("department").get("id"), departmentId);
    }

    public static Specification<Project> hasStatus(ProjectStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    public static Specification<Project> hasPriority(Priority priority) {
        if (priority == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("priority"), priority);
    }

    public static Specification<Project> searchTermMatches(String searchTerm) {
        if (!StringUtils.hasText(searchTerm)) {
            return null;
        }
        String likePattern = "%" + searchTerm.toLowerCase() + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("name")), likePattern),
                builder.like(builder.lower(root.get("code")), likePattern)
        );
    }

    @SafeVarargs
    public static Specification<Project> combine(Specification<Project> base,
                                                  Specification<Project>... additional) {
        Specification<Project> result = base;
        for (Specification<Project> spec : additional) {
            if (spec != null) {
                result = result == null ? Specification.where(spec) : result.and(spec);
            }
        }
        return result;
    }
}
