package com.controlplane.backend.controller;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.user.AssignRolesRequest;
import com.controlplane.backend.dto.user.CreateUserRequest;
import com.controlplane.backend.dto.user.UpdateProfileRequest;
import com.controlplane.backend.dto.user.UpdateUserRequest;
import com.controlplane.backend.dto.user.UserResponse;
import com.controlplane.backend.entity.enums.UserStatus;
import com.controlplane.backend.security.CustomUserDetails;
import com.controlplane.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Management", description = "Administrative CRUD, search and role assignment for user accounts")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Search users with pagination, sorting and filtering")
    public ResponseEntity<PageResponse<UserResponse>> search(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false, defaultValue = "false") boolean includeDeleted,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(userService.search(searchTerm, status, includeDeleted, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Get a user by id")
    public ResponseEntity<UserResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    @Operation(summary = "Create a user account directly, bypassing self-registration and email verification")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Update a user's core details")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the authenticated user's own profile")
    public ResponseEntity<UserResponse> updateOwnProfile(@AuthenticationPrincipal CustomUserDetails principal,
                                                          @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateOwnProfile(principal.getId(), request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @Operation(summary = "Soft delete a user account")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        userService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Restore a soft deleted user account")
    public ResponseEntity<Void> restore(@PathVariable UUID id) {
        userService.restore(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Enable a user account")
    public ResponseEntity<Void> enable(@PathVariable UUID id) {
        userService.enable(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Disable a user account")
    public ResponseEntity<Void> disable(@PathVariable UUID id) {
        userService.disable(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Assign one or more roles to a user")
    public ResponseEntity<UserResponse> assignRoles(@PathVariable UUID id,
                                                     @Valid @RequestBody AssignRolesRequest request) {
        return ResponseEntity.ok(userService.assignRoles(id, request));
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Remove a role from a user")
    public ResponseEntity<UserResponse> removeRole(@PathVariable UUID id, @PathVariable UUID roleId) {
        return ResponseEntity.ok(userService.removeRole(id, roleId));
    }
}
