package com.controlplane.backend.scheduler;

import com.controlplane.backend.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupSchedulerTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenCleanupScheduler scheduler;

    @Test
    void purgesTokensExpiredBeforeTheCutoff() {
        when(refreshTokenRepository.deleteAllExpiredBefore(any(Instant.class))).thenReturn(3);

        scheduler.purgeExpiredTokens();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(refreshTokenRepository).deleteAllExpiredBefore(cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isBefore(Instant.now());
    }
}
