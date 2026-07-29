package com.controlplane.backend.exception;

import org.springframework.http.HttpStatus;

public class OrganizationNotEmptyException extends BusinessException {

    public OrganizationNotEmptyException(long departmentCount) {
        super("Cannot delete an organization that still has " + departmentCount
                        + " department(s). Remove or reassign them first.",
                HttpStatus.CONFLICT, "ORGANIZATION_NOT_EMPTY");
    }
}
