package com.controlplane.backend.repository.spec;

import com.controlplane.backend.entity.User;
import com.controlplane.backend.entity.enums.UserStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> notDeleted() {
        return (root, query, builder) -> builder.isFalse(root.get("deleted"));
    }

    public static Specification<User> onlyDeleted() {
        return (root, query, builder) -> builder.isTrue(root.get("deleted"));
    }

    public static Specification<User> hasStatus(UserStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    public static Specification<User> searchTermMatches(String searchTerm) {
        if (!StringUtils.hasText(searchTerm)) {
            return null;
        }
        String likePattern = "%" + searchTerm.toLowerCase() + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("username")), likePattern),
                builder.like(builder.lower(root.get("email")), likePattern),
                builder.like(builder.lower(root.get("firstName")), likePattern),
                builder.like(builder.lower(root.get("lastName")), likePattern)
        );
    }

    public static Specification<User> combine(Specification<User> base, Specification<User>... additional) {
        Specification<User> result = base;
        for (Specification<User> spec : additional) {
            if (spec != null) {
                result = result == null ? Specification.where(spec) : result.and(spec);
            }
        }
        return result;
    }
}
