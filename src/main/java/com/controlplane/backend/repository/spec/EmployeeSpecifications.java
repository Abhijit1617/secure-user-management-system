package com.controlplane.backend.repository.spec;

import com.controlplane.backend.entity.Employee;
import com.controlplane.backend.entity.Organization;
import com.controlplane.backend.entity.enums.EmployeeStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.UUID;

public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> belongsToOrganization(Organization organization) {
        return (root, query, builder) -> builder.equal(root.get("organization"), organization);
    }

    public static Specification<Employee> hasDepartment(UUID departmentId) {
        if (departmentId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("department").get("id"), departmentId);
    }

    public static Specification<Employee> hasStatus(EmployeeStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    public static Specification<Employee> searchTermMatches(String searchTerm) {
        if (!StringUtils.hasText(searchTerm)) {
            return null;
        }
        String likePattern = "%" + searchTerm.toLowerCase() + "%";
        return (root, query, builder) -> {
            Join<Object, Object> user = root.join("user", JoinType.LEFT);
            return builder.or(
                    builder.like(builder.lower(root.get("employeeCode")), likePattern),
                    builder.like(builder.lower(root.get("designation")), likePattern),
                    builder.like(builder.lower(user.get("firstName")), likePattern),
                    builder.like(builder.lower(user.get("lastName")), likePattern),
                    builder.like(builder.lower(user.get("email")), likePattern)
            );
        };
    }

    @SafeVarargs
    public static Specification<Employee> combine(Specification<Employee> base,
                                                    Specification<Employee>... additional) {
        Specification<Employee> result = base;
        for (Specification<Employee> spec : additional) {
            if (spec != null) {
                result = result == null ? Specification.where(spec) : result.and(spec);
            }
        }
        return result;
    }
}
