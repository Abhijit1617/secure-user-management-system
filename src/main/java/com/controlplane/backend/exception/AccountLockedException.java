package com.controlplane.backend.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class AccountLockedException extends BusinessException {

    public AccountLockedException(Instant lockedUntil) {
        super("Account is locked until " + DateTimeFormatter.ISO_INSTANT.format(lockedUntil)
                        + " due to repeated failed login attempts",
                HttpStatus.LOCKED, "ACCOUNT_LOCKED");
    }
}
