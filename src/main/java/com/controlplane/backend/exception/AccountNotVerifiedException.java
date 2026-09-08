package com.controlplane.backend.exception;

import org.springframework.http.HttpStatus;

public class AccountNotVerifiedException extends BusinessException {

    public AccountNotVerifiedException() {
        super("Please verify your email address before logging in", HttpStatus.FORBIDDEN, "ACCOUNT_NOT_VERIFIED");
    }
}
