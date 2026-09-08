package com.controlplane.backend.service.impl;

import com.controlplane.backend.dto.common.PageResponse;
import com.controlplane.backend.dto.user.AssignRolesRequest;
import com.controlplane.backend.dto.user.CreateUserRequest;
import com.controlplane.backend.dto.user.UpdateProfileRequest;
import com.controlplane.backend.dto.user.UpdateUserRequest;
import com.controlplane.backend.dto.user.UserResponse;
import com.controlplane.backend.entity.Role;
import com.controlplane.backend.entity.User;
import com.controlplane.backend.entity.enums.UserStatus;
import com.controlplane.backend.exception.DuplicateResourceException;
import com.controlplane.backend.exception.ResourceNotFoundException;
import com.controlplane.backend.mapper.UserMapper;
import com.controlplane.backend.repository.RoleRepository;
import com.controlplane.backend.repository.UserRepository;
import com.controlplane.backend.repository.spec.UserSpecifications;
import com.controlplane.backend.service.RefreshTokenService;
import com.controlplane.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.controlplane.backend.config.RedisCacheConfig.USER_CACHE;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String searchTerm, UserStatus status, boolean includeDeleted,
                                              Pageable pageable) {
        Specification<User> spec = UserSpecifications.combine(
                includeDeleted ? null : UserSpecifications.notDeleted(),
                UserSpecifications.hasStatus(status),
                UserSpecifications.searchTermMatches(searchTerm)
        );
        Page<User> page = userRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(userMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = USER_CACHE, key = "#id")
    public UserResponse getById(UUID id) {
        return userMapper.toResponse(loadUser(id));
    }

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw DuplicateResourceException.of("User", "username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw DuplicateResourceException.of("User", "email", request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();

        for (String roleName : request.getRoleNames()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> ResourceNotFoundException.of("Role", "name", roleName));
            user.addRole(role);
        }

        user = userRepository.save(user);
        log.info("Administrator created user account [{}]", user.getUsername());
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = USER_CACHE, key = "#id")
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = loadUser(id);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = USER_CACHE, key = "#id")
    public UserResponse updateOwnProfile(UUID id, UpdateProfileRequest request) {
        User user = loadUser(id);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = USER_CACHE, key = "#id")
    public void softDelete(UUID id) {
        User user = loadUser(id);
        user.setDeleted(true);
        user.setDeletedAt(Instant.now());
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user);
        log.info("User [{}] was soft deleted", user.getUsername());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = USER_CACHE, key = "#id")
    public void restore(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", "id", id));
        user.setDeleted(false);
        user.setDeletedAt(null);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("User [{}] was restored", user.getUsername());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = USER_CACHE, key = "#id")
    public void enable(UUID id) {
        User user = loadUser(id);
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
        log.info("User [{}] was enabled", user.getUsername());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = USER_CACHE, key = "#id")
    public void disable(UUID id) {
        User user = loadUser(id);
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user);
        log.info("User [{}] was disabled", user.getUsername());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = USER_CACHE, key = "#id")
    public UserResponse assignRoles(UUID id, AssignRolesRequest request) {
        User user = loadUser(id);
        Set<Role> roles = new HashSet<>();
        for (UUID roleId : request.getRoleIds()) {
            roles.add(roleRepository.findById(roleId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Role", "id", roleId)));
        }
        roles.forEach(user::addRole);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = USER_CACHE, key = "#id")
    public UserResponse removeRole(UUID id, UUID roleId) {
        User user = loadUser(id);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", "id", roleId));
        user.removeRole(role);
        return userMapper.toResponse(userRepository.save(user));
    }

    private User loadUser(UUID id) {
        return userRepository.findById(id)
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> ResourceNotFoundException.of("User", "id", id));
    }
}
