package com.controlplane.backend.scheduler;

import com.controlplane.backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Refresh tokens are never deleted at the point they expire or are
 * revoked — {@code RefreshTokenServiceImpl} only flips {@code revoked}
 * to {@code true} so replay attempts can still be detected and logged.
 * Left unattended, that table grows without bound. This job removes rows
 * that expired more than 24 hours ago, which is long enough to be well
 * past their useful life for replay detection.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupScheduler.class);

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        Instant cutoff = Instant.now().minusSeconds(24 * 60 * 60);
        int deleted = refreshTokenRepository.deleteAllExpiredBefore(cutoff);
        if (deleted > 0) {
            log.info("Purged {} expired refresh token(s) older than {}", deleted, cutoff);
        }
    }
}
