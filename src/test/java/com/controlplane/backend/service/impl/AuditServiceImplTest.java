package com.controlplane.backend.service.impl;

import com.controlplane.backend.entity.AuditLog;
import com.controlplane.backend.entity.User;
import com.controlplane.backend.entity.enums.AuditAction;
import com.controlplane.backend.event.AuditEvent;
import com.controlplane.backend.mapper.AuditMapper;
import com.controlplane.backend.repository.AuditLogRepository;
import com.controlplane.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditMapper auditMapper;

    @InjectMocks
    private AuditServiceImpl auditService;

    @BeforeEach
    void setUp() {
        lenient().when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void recordEventPersistsAuditLogWithResolvedUser() {
        UUID userId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        User user = User.builder().username("jane.doe").build();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        AuditEvent event = AuditEvent.of(userId, AuditAction.LOGIN, "User", entityId, "127.0.0.1", "JUnit-Agent");
        auditService.recordEvent(event);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.LOGIN);
        assertThat(captor.getValue().getEntityName()).isEqualTo("User");
        assertThat(captor.getValue().getIpAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void recordEventToleratesUnresolvableUser() {
        UUID unknownUserId = UUID.randomUUID();
        when(userRepository.findById(unknownUserId)).thenReturn(Optional.empty());

        AuditEvent event = AuditEvent.of(unknownUserId, AuditAction.LOGIN_FAILED, "User", null, "127.0.0.1", null);
        auditService.recordEvent(event);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isNull();
    }
}
