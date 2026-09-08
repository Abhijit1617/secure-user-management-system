package com.controlplane.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class SecurityProperties {

    private int maxFailedLoginAttempts;

    private int accountLockDurationMinutes;

    private int passwordResetTokenExpirationMinutes;

    private int emailVerificationTokenExpirationHours;
}
