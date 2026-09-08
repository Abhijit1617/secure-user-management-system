package com.controlplane.backend.mapper;

import com.controlplane.backend.dto.department.DepartmentResponse;
import com.controlplane.backend.dto.department.DepartmentTreeResponse;
import com.controlplane.backend.dto.department.EmployeeRefResponse;
import com.controlplane.backend.entity.Department;
import com.controlplane.backend.entity.Employee;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DepartmentMapper {

    public DepartmentResponse toResponse(Department department, int employeeCount, int childCount) {
        if (department == null) {
            return null;
        }
        Department parent = department.getParentDepartment();
        return DepartmentResponse.builder()
                .id(department.getId())
                .organizationId(department.getOrganization().getId())
                .name(department.getName())
                .code(department.getCode())
                .description(department.getDescription())
                .parentDepartmentId(parent != null ? parent.getId() : null)
                .parentDepartmentName(parent != null ? parent.getName() : null)
                .head(toEmployeeRef(department.getHead()))
                .active(department.isActive())
                .employeeCount(employeeCount)
                .childDepartmentCount(childCount)
                .createdAt(department.getCreatedAt())
                .build();
    }

    public DepartmentTreeResponse toTreeNode(Department department, int employeeCount,
                                              List<DepartmentTreeResponse> children) {
        return DepartmentTreeResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .code(department.getCode())
                .active(department.isActive())
                .employeeCount(employeeCount)
                .children(children)
                .build();
    }

    private EmployeeRefResponse toEmployeeRef(Employee employee) {
        if (employee == null) {
            return null;
        }
        return EmployeeRefResponse.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .fullName(employee.getUser() != null ? employee.getUser().getFullName() : null)
                .build();
    }
}
