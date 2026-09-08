package com.controlplane.backend.repository.spec;

import com.controlplane.backend.entity.Task;
import com.controlplane.backend.entity.enums.TaskStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.UUID;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> belongsToProject(UUID projectId) {
        if (projectId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Task> hasAssignee(UUID assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("assignee").get("id"), assigneeId);
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    public static Specification<Task> isOverdue() {
        return (root, query, builder) -> builder.and(
                builder.lessThan(root.get("dueDate"), LocalDate.now()),
                builder.notEqual(root.get("status"), TaskStatus.DONE)
        );
    }

    public static Specification<Task> searchTermMatches(String searchTerm) {
        if (!StringUtils.hasText(searchTerm)) {
            return null;
        }
        String likePattern = "%" + searchTerm.toLowerCase() + "%";
        return (root, query, builder) -> builder.like(builder.lower(root.get("title")), likePattern);
    }

    @SafeVarargs
    public static Specification<Task> combine(Specification<Task> base, Specification<Task>... additional) {
        Specification<Task> result = base;
        for (Specification<Task> spec : additional) {
            if (spec != null) {
                result = result == null ? Specification.where(spec) : result.and(spec);
            }
        }
        return result;
    }
}
