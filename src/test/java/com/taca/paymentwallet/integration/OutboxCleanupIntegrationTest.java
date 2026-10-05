package com.taca.paymentwallet.integration;

import com.taca.paymentwallet.application.port.out.OutboxCleanupPort;
import com.taca.paymentwallet.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@TestPropertySource(
        properties = "spring.jpa.hibernate.ddl-auto=validate"
)
class OutboxCleanupIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName(
                            "outbox_cleanup_test"
                    )
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                MYSQL::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                MYSQL::getUsername
        );

        registry.add(
                "spring.datasource.password",
                MYSQL::getPassword
        );

        registry.add(
                "spring.flyway.enabled",
                () -> "true"
        );
    }

    @Autowired
    private OutboxEventJpaRepository repository;

    private OutboxCleanupPort cleanupPort;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.flush();

        cleanupPort =
                new com.taca.paymentwallet.infrastructure.persistence.adapter
                        .OutboxCleanupPersistenceAdapter(
                        repository
                );
    }

    @Test
    void shouldDeleteOnlyCompletedEventsOlderThanCutoff() {
        Instant cutoff =
                Instant.parse(
                        "2026-10-01T00:00:00Z"
                );

        UUID oldPublished =
                save(
                        "payment.created",
                        instant(
                                "2026-09-20T00:00:00Z"
                        ),
                        instant(
                                "2026-09-20T00:01:00Z"
                        ),
                        null
                );

        UUID oldDeadLettered =
                save(
                        "wallet.allocated",
                        instant(
                                "2026-09-20T00:00:00Z"
                        ),
                        null,
                        instant(
                                "2026-09-21T00:00:00Z"
                        )
                );

        UUID oldPending =
                save(
                        "payment.created",
                        instant(
                                "2026-09-10T00:00:00Z"
                        ),
                        null,
                        null
                );

        UUID recentPublished =
                save(
                        "payment.created",
                        instant(
                                "2026-10-03T00:00:00Z"
                        ),
                        instant(
                                "2026-10-03T00:01:00Z"
                        ),
                        null
                );

        int deleted =
                cleanupPort.deleteCompletedBefore(
                        cutoff,
                        100
                );

        assertThat(deleted)
                .isEqualTo(2);

        assertThat(repository.existsById(oldPublished))
                .isFalse();

        assertThat(repository.existsById(oldDeadLettered))
                .isFalse();

        assertThat(repository.existsById(oldPending))
                .isTrue();

        assertThat(repository.existsById(recentPublished))
                .isTrue();
    }

    @Test
    void shouldRespectCleanupBatchSize() {
        Instant cutoff =
                Instant.parse(
                        "2026-10-01T00:00:00Z"
                );

        for (int i = 0; i < 5; i++) {
            save(
                    "payment.created",
                    instant(
                            "2026-09-20T00:00:00Z"
                    ),
                    instant(
                            "2026-09-20T00:01:00Z"
                    ),
                    null
            );
        }

        int deleted =
                cleanupPort.deleteCompletedBefore(
                        cutoff,
                        2
                );

        assertThat(deleted)
                .isEqualTo(2);

        assertThat(repository.count())
                .isEqualTo(3);
    }

    private UUID save(
            String eventType,
            LocalDateTime occurredAt,
            LocalDateTime publishedAt,
            LocalDateTime deadLetteredAt
    ) {
        UUID id = UUID.randomUUID();

        OutboxEventJpaEntity entity =
                new OutboxEventJpaEntity();

        entity.setId(id);
        entity.setAggregateType("PAYMENT");
        entity.setAggregateId(
                UUID.randomUUID()
        );
        entity.setEventType(eventType);
        entity.setPayload("{}");
        entity.setOccurredAt(occurredAt);
        entity.setPublishedAt(publishedAt);
        entity.setRetryCount(
                deadLetteredAt == null
                        ? 0
                        : 3
        );
        entity.setNextAttemptAt(null);
        entity.setDeadLetteredAt(
                deadLetteredAt
        );
        entity.setLastError(null);

        repository.saveAndFlush(entity);

        return id;
    }

    private LocalDateTime instant(
            String value
    ) {
        return LocalDateTime.ofInstant(
                Instant.parse(value),
                ZoneOffset.UTC
        );
    }
}