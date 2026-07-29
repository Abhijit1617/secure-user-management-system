package com.controlplane.backend.entity.enums;

public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    LOGIN,
    LOGOUT,
    LOGIN_FAILED,
    PASSWORD_CHANGE,
    ROLE_ASSIGNED,
    ROLE_REVOKED
}
