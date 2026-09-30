package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.OutboxEventJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class OutboxLockingIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("outbox_lock_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private OutboxEventJpaRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanOutbox() {
        repository.deleteAll();
        repository.flush();
    }

    @Test
    void shouldLockPendingEventsInOrder() {
        UUID firstId = insertEvent(LocalDateTime.of(2026, 9, 30, 10, 0));
        UUID secondId = insertEvent(LocalDateTime.of(2026, 9, 30, 10, 1));

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        List<OutboxEventJpaEntity> events =
                transaction.execute(status -> repository.lockNextBatch(10, 3));

        assertThat(events)
                .extracting(OutboxEventJpaEntity::getId)
                .containsExactly(firstId, secondId);
    }

    @Test
    void shouldExcludePublishedAndRetryExhaustedEvents() {
        insertEvent(LocalDateTime.of(2026, 9, 30, 10, 0));

        OutboxEventJpaEntity published =
                newEvent(LocalDateTime.of(2026, 9, 30, 10, 1));
        published.setPublishedAt(LocalDateTime.of(2026, 9, 30, 10, 2));
        repository.saveAndFlush(published);

        OutboxEventJpaEntity exhausted =
                newEvent(LocalDateTime.of(2026, 9, 30, 10, 3));
        exhausted.setRetryCount(3);
        repository.saveAndFlush(exhausted);

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        List<OutboxEventJpaEntity> events =
                transaction.execute(status -> repository.lockNextBatch(10, 3));

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getPublishedAt()).isNull();
        assertThat(events.getFirst().getRetryCount()).isZero();
    }

    private UUID insertEvent(LocalDateTime occurredAt) {
        OutboxEventJpaEntity entity = newEvent(occurredAt);
        repository.saveAndFlush(entity);
        return entity.getId();
    }

    private OutboxEventJpaEntity newEvent(LocalDateTime occurredAt) {
        OutboxEventJpaEntity entity = new OutboxEventJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setAggregateType("PAYMENT");
        entity.setAggregateId(UUID.randomUUID());
        entity.setEventType("payment.succeeded");
        entity.setPayload("{}");
        entity.setHeaders(null);
        entity.setOccurredAt(occurredAt);
        entity.setPublishedAt(null);
        entity.setRetryCount(0);
        entity.setLastError(null);
        return entity;
    }
}