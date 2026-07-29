package com.controlplane.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * Base64 encoded HMAC-SHA256 signing secret. Must be rotated in
     * production; the default in application.yml is for local development
     * only.
     */
    private String secret;

    private long accessTokenExpirationMs;

    private long refreshTokenExpirationMs;

    private String issuer;
}
