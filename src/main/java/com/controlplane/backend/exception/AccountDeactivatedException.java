package com.controlplane.backend.exception;

import org.springframework.http.HttpStatus;

public class AccountDeactivatedException extends BusinessException {

    public AccountDeactivatedException() {
        super("This account has been deactivated. Contact an administrator to reactivate it",
                HttpStatus.FORBIDDEN, "ACCOUNT_DEACTIVATED");
    }
}
