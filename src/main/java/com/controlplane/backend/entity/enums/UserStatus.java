package com.controlplane.backend.entity.enums;

/**
 * Represents the lifecycle state of a user account within the platform.
 */
public enum UserStatus {

    /**
     * Account has been created but the email address has not yet been verified.
     */
    PENDING_VERIFICATION,

    /**
     * Account is fully active and can authenticate normally.
     */
    ACTIVE,

    /**
     * Account has been temporarily disabled by an administrator.
     */
    INACTIVE,

    /**
     * Account has been locked out, typically after repeated failed login attempts.
     */
    LOCKED,

    /**
     * Account has been permanently disabled and can no longer authenticate.
     */
    DEACTIVATED
}
