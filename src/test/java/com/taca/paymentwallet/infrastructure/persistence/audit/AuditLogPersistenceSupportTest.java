package com.taca.paymentwallet.infrastructure.persistence.audit;

import com.taca.paymentwallet.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.AuditLogJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditLogPersistenceSupportTest {

    private static final Instant OCCURRED_AT =
            Instant.parse(
                    "2026-09-25T06:45:00Z"
            );

    private AuditLogJpaRepository repository;

    private AuditLogPersistenceSupport support;

    @BeforeEach
    void setUp() {
        repository =
                mock(AuditLogJpaRepository.class);

        support =
                new AuditLogPersistenceSupport(
                        repository,
                        new PersistenceUuidGenerator(),
                        new ObjectMapper()
                );
    }

    @Test
    void shouldAppendSystemAuditLog() {
        UUID targetId =
                UUID.randomUUID();

        AuditLogRecord record =
                new AuditLogRecord(
                        null,
                        AuditActorType.SYSTEM,
                        "SETTLEMENT_RUN",
                        "SETTLEMENT_BATCH",
                        targetId,
                        "Automatic settlement",
                        """
                        {
                          "source": "scheduler"
                        }
                        """,
                        OCCURRED_AT
                );

        support.append(record);

        ArgumentCaptor<AuditLogJpaEntity>
                captor =
                ArgumentCaptor.forClass(
                        AuditLogJpaEntity.class
                );

        verify(repository)
                .save(
                        captor.capture()
                );

        AuditLogJpaEntity entity =
                captor.getValue();

        assertEquals(
                7,
                entity.getId().version()
        );

        assertNull(
                entity.getActorUserId()
        );

        assertEquals(
                "SYSTEM",
                entity.getActorType()
        );

        assertEquals(
                "SETTLEMENT_RUN",
                entity.getAction()
        );

        assertEquals(
                "SETTLEMENT_BATCH",
                entity.getTargetType()
        );

        assertEquals(
                targetId,
                entity.getTargetId()
        );

        assertEquals(
                "Automatic settlement",
                entity.getReason()
        );

        assertNotNull(
                entity.getMetadata()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        6,
                        45
                ),
                entity.getOccurredAt()
        );
    }

    @Test
    void shouldAppendUserAuditLog() {
        UUID userId =
                UUID.randomUUID();

        UUID targetId =
                UUID.randomUUID();

        AuditLogRecord record =
                new AuditLogRecord(
                        userId,
                        AuditActorType.USER,
                        "REQUEST_PAYOUT",
                        "PAYOUT",
                        targetId,
                        null,
                        null,
                        OCCURRED_AT
                );

        support.append(record);

        verify(repository)
                .save(
                        argThat(entity ->
                                userId.equals(
                                        entity.getActorUserId()
                                )
                                        && "USER".equals(
                                        entity.getActorType()
                                )
                                        && targetId.equals(
                                        entity.getTargetId()
                                )
                        )
                );
    }

    @Test
    void shouldAppendAdminAuditLog() {
        UUID adminId =
                UUID.randomUUID();

        AuditLogRecord record =
                new AuditLogRecord(
                        adminId,
                        AuditActorType.ADMIN,
                        "UPDATE_FEE_CONFIG",
                        "FEE_CONFIG",
                        UUID.randomUUID(),
                        "Fee configuration changed",
                        """
                        {
                          "oldRateBps": 700,
                          "newRateBps": 800
                        }
                        """,
                        OCCURRED_AT
                );

        support.append(record);

        verify(repository)
                .save(
                        argThat(entity ->
                                "ADMIN".equals(
                                        entity.getActorType()
                                )
                                        && adminId.equals(
                                        entity.getActorUserId()
                                )
                        )
                );
    }

    @Test
    void shouldRejectInvalidJsonMetadata() {
        AuditLogRecord record =
                new AuditLogRecord(
                        null,
                        AuditActorType.SYSTEM,
                        "TEST",
                        "DATABASE",
                        UUID.randomUUID(),
                        null,
                        "not-json",
                        OCCURRED_AT
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        support.append(record)
        );

        verifyNoInteractions(
                repository
        );
    }

    @Test
    void shouldRejectSystemActorWithUserId() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new AuditLogRecord(
                                UUID.randomUUID(),
                                AuditActorType.SYSTEM,
                                "TEST",
                                "DATABASE",
                                UUID.randomUUID(),
                                null,
                                null,
                                OCCURRED_AT
                        )
        );
    }

    @Test
    void shouldRejectUserActorWithoutUserId() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new AuditLogRecord(
                                null,
                                AuditActorType.USER,
                                "TEST",
                                "DATABASE",
                                UUID.randomUUID(),
                                null,
                                null,
                                OCCURRED_AT
                        )
        );
    }

    @Test
    void shouldRejectAdminActorWithoutUserId() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new AuditLogRecord(
                                null,
                                AuditActorType.ADMIN,
                                "TEST",
                                "DATABASE",
                                UUID.randomUUID(),
                                null,
                                null,
                                OCCURRED_AT
                        )
        );
    }
}