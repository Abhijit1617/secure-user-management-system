package com.controlplane.backend.mapper;

import com.controlplane.backend.dto.auth.CurrentUserResponse;
import com.controlplane.backend.dto.user.UserResponse;
import com.controlplane.backend.dto.user.UserSummaryResponse;
import com.controlplane.backend.entity.Permission;
import com.controlplane.backend.entity.Role;
import com.controlplane.backend.entity.User;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .roles(roleNames(user))
                .build();
    }

    public UserSummaryResponse toSummaryResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus().name())
                .build();
    }

    public CurrentUserResponse toCurrentUserResponse(User user) {
        if (user == null) {
            return null;
        }
        return CurrentUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .roles(roleNames(user))
                .permissions(permissionNames(user))
                .build();
    }

    private List<String> roleNames(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .toList();
    }

    private List<String> permissionNames(User user) {
        Set<String> permissions = new LinkedHashSet<>();
        for (Role role : user.getRoles()) {
            for (Permission permission : role.getPermissions()) {
                permissions.add(permission.getName());
            }
        }
        return permissions.stream().sorted(Comparator.naturalOrder()).toList();
    }
}
