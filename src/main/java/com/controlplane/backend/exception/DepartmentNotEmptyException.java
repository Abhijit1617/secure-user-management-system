package com.controlplane.backend.exception;

import org.springframework.http.HttpStatus;

public class DepartmentNotEmptyException extends BusinessException {

    public DepartmentNotEmptyException(long employeeCount, long childCount) {
        super("Cannot delete a department with " + employeeCount + " employee(s) and "
                        + childCount + " sub-department(s) still attached to it.",
                HttpStatus.CONFLICT, "DEPARTMENT_NOT_EMPTY");
    }
}
