package com.controlplane.backend.security;

import com.controlplane.backend.entity.User;
import com.controlplane.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves a {@link UserDetails} instance by username or email, backing the
 * whole authentication flow. Accepting either identifier at the same login
 * field is a small usability detail users expect and costs nothing extra
 * here since {@link UserRepository#findByUsernameOrEmail(String)} already
 * covers it with a single indexed lookup on either column.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No account found for username or email: " + usernameOrEmail));
        return CustomUserDetails.fromUser(user);
    }
}
