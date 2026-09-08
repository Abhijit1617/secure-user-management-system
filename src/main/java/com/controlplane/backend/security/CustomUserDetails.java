package com.controlplane.backend.security;

import com.controlplane.backend.entity.Permission;
import com.controlplane.backend.entity.Role;
import com.controlplane.backend.entity.User;
import com.controlplane.backend.entity.enums.UserStatus;
import com.controlplane.backend.security.jwt.JwtClaims;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Adapts a {@link User} entity to the {@link UserDetails} contract Spring
 * Security operates on. Authorities are the union of every role name
 * (prefixed with {@code ROLE_}) and every permission name granted through
 * those roles, which lets controllers authorize with either
 * {@code hasRole(...)} or {@code hasAuthority(...)} depending on how
 * coarse or fine grained the check needs to be.
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String username;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String password;
    private final UserStatus status;
    private final boolean emailVerified;
    private final Instant accountLockedUntil;
    private final Collection<? extends GrantedAuthority> authorities;

    private CustomUserDetails(UUID id, String username, String email, String firstName, String lastName,
                               String password, UserStatus status, boolean emailVerified,
                               Instant accountLockedUntil, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.status = status;
        this.emailVerified = emailVerified;
        this.accountLockedUntil = accountLockedUntil;
        this.authorities = authorities;
    }

    public static CustomUserDetails fromUser(User user) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + role.getName()));
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            }
        }
        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPassword(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getAccountLockedUntil(),
                authorities
        );
    }

    /**
     * Builds a principal directly from an already-validated access token's
     * claims, without touching the database. This is what makes the
     * authentication filter genuinely stateless: every field Spring
     * Security's authorization checks need — id, username, authorities —
     * was already embedded in the token at login time.
     */
    public static CustomUserDetails fromClaims(JwtClaims claims) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (String role : safe(claims.roles())) {
            authorities.add(new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + role));
        }
        for (String permission : safe(claims.permissions())) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        return new CustomUserDetails(
                claims.userId(),
                claims.username(),
                null,
                null,
                null,
                null,
                UserStatus.ACTIVE,
                true,
                null,
                authorities
        );
    }

    private static List<String> safe(List<String> values) {
        return values == null ? List.of() : values;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (status == UserStatus.LOCKED) {
            return accountLockedUntil != null && Instant.now().isAfter(accountLockedUntil);
        }
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
