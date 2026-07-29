package com.controlplane.backend.controller;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.department.AssignDepartmentManagerRequest;
import com.controlplane.backend.dto.department.CreateDepartmentRequest;
import com.controlplane.backend.dto.department.DepartmentResponse;
import com.controlplane.backend.dto.department.DepartmentStatisticsResponse;
import com.controlplane.backend.dto.department.DepartmentTreeResponse;
import com.controlplane.backend.dto.department.UpdateDepartmentRequest;
import com.controlplane.backend.service.DepartmentService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Department Management", description = "Department CRUD, hierarchy, manager assignment and statistics")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('DEPARTMENT_READ')")
    @Operation(summary = "Search departments within an organization, with pagination")
    public ResponseEntity<PageResponse<DepartmentResponse>> search(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) String searchTerm,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(departmentService.search(organizationId, searchTerm, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPARTMENT_READ')")
    @Operation(summary = "Get a department by id")
    public ResponseEntity<DepartmentResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.getById(id));
    }

    @GetMapping("/hierarchy")
    @PreAuthorize("hasAuthority('DEPARTMENT_READ')")
    @Operation(summary = "Get the full department hierarchy for an organization as a tree")
    public ResponseEntity<List<DepartmentTreeResponse>> getHierarchy(@RequestParam UUID organizationId) {
        return ResponseEntity.ok(departmentService.getHierarchy(organizationId));
    }

    @GetMapping("/{id}/statistics")
    @PreAuthorize("hasAuthority('DEPARTMENT_READ')")
    @Operation(summary = "Get employee, project and sub-department counts for a department")
    public ResponseEntity<DepartmentStatisticsResponse> getStatistics(@PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.getStatistics(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DEPARTMENT_CREATE')")
    @Operation(summary = "Create a new department, optionally nested under a parent")
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody CreateDepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPARTMENT_UPDATE')")
    @Operation(summary = "Update a department's details or reparent it")
    public ResponseEntity<DepartmentResponse> update(@PathVariable UUID id,
                                                      @Valid @RequestBody UpdateDepartmentRequest request) {
        return ResponseEntity.ok(departmentService.update(id, request));
    }

    @PutMapping("/{id}/manager")
    @PreAuthorize("hasAuthority('DEPARTMENT_UPDATE')")
    @Operation(summary = "Assign an employee as the department's head/manager")
    public ResponseEntity<DepartmentResponse> assignManager(
            @PathVariable UUID id, @Valid @RequestBody AssignDepartmentManagerRequest request) {
        return ResponseEntity.ok(departmentService.assignManager(id, request));
    }

    @DeleteMapping("/{id}/manager")
    @PreAuthorize("hasAuthority('DEPARTMENT_UPDATE')")
    @Operation(summary = "Remove the department's assigned manager")
    public ResponseEntity<DepartmentResponse> removeManager(@PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.removeManager(id));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('DEPARTMENT_UPDATE')")
    @Operation(summary = "Reactivate a deactivated department")
    public ResponseEntity<DepartmentResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.activate(id));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('DEPARTMENT_UPDATE')")
    @Operation(summary = "Deactivate a department")
    public ResponseEntity<DepartmentResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPARTMENT_DELETE')")
    @Operation(summary = "Delete a department",
            description = "Fails with 409 Conflict if the department still has employees or sub-departments.")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
