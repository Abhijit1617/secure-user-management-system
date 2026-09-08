package com.controlplane.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
/**
 * Expresses the seniority ordering documented in
 * {@code docs/security-architecture.md}: a user holding a senior role
 * automatically satisfies {@code hasRole(...)} checks written for any role
 * beneath it, without every {@code @PreAuthorize} annotation having to
 * enumerate every role that should be allowed.
 * <p>
 * Both beans are declared {@code static} per Spring Security's guidance for
 * {@code @EnableMethodSecurity}, since {@link MethodSecurityExpressionHandler}
 * participates in bean post-processing and must be available before the
 * rest of the application context finishes refreshing.
 */
@Configuration
public class RoleHierarchyConfig {

    private static final String HIERARCHY = """
            ROLE_SUPER_ADMIN > ROLE_ADMIN
            ROLE_ADMIN > ROLE_MANAGER
            ROLE_MANAGER > ROLE_EMPLOYEE
            ROLE_EMPLOYEE > ROLE_VIEWER
            """;

    @Bean
    public static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy(HIERARCHY);
    }

    @Bean
    public static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }
}
