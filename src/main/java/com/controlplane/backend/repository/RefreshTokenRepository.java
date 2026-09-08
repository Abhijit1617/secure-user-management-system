package com.controlplane.backend.repository;

import com.controlplane.backend.entity.RefreshToken;
import com.controlplane.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByUserAndRevokedFalse(User user);

    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.user = :user and t.revoked = false")
    int revokeAllForUser(@Param("user") User user);

    @Modifying
    @Query("delete from RefreshToken t where t.expiryDate < :cutoff")
    int deleteAllExpiredBefore(@Param("cutoff") Instant cutoff);
}
