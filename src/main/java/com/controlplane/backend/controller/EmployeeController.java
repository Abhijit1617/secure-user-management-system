package com.controlplane.backend.controller;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.employee.CreateEmployeeRequest;
import com.controlplane.backend.dto.employee.EmployeeResponse;
import com.controlplane.backend.dto.employee.UpdateEmployeeProfileRequest;
import com.controlplane.backend.dto.employee.UpdateEmployeeRequest;
import com.controlplane.backend.dto.employee.UpdateEmployeeStatusRequest;
import com.controlplane.backend.entity.enums.EmployeeStatus;
import com.controlplane.backend.service.EmployeeService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Employee Management", description = "Employee CRUD, profile, address, emergency contact and status")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    @Operation(summary = "Search employees within an organization, with department/status filters and pagination")
    public ResponseEntity<PageResponse<EmployeeResponse>> search(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) String searchTerm,
            @PageableDefault(size = 20, sort = "employeeCode") Pageable pageable) {
        return ResponseEntity.ok(employeeService.search(organizationId, departmentId, status, searchTerm, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    @Operation(summary = "Get an employee by id, including address and emergency contact")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(employeeService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_CREATE')")
    @Operation(summary = "Create an employee record linked to an existing user account")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE')")
    @Operation(summary = "Update an employee's department, designation, employment type and manager")
    public ResponseEntity<EmployeeResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @PutMapping("/{id}/profile")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE')")
    @Operation(summary = "Update an employee's address and emergency contact")
    public ResponseEntity<EmployeeResponse> updateProfile(@PathVariable UUID id,
                                                           @Valid @RequestBody UpdateEmployeeProfileRequest request) {
        return ResponseEntity.ok(employeeService.updateProfile(id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE')")
    @Operation(summary = "Change an employee's status (active, on leave, suspended, resigned, terminated)")
    public ResponseEntity<EmployeeResponse> updateStatus(@PathVariable UUID id,
                                                          @Valid @RequestBody UpdateEmployeeStatusRequest request) {
        return ResponseEntity.ok(employeeService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_DELETE')")
    @Operation(summary = "Delete an employee record")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
