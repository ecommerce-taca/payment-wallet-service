package com.taca.paymentwallet.integration;

import com.taca.paymentwallet.infrastructure.health.OutboxBacklogHealthIndicator;
import com.taca.paymentwallet.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class OutboxBacklogHealthIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName(
                            "outbox_health_test"
                    )
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(
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
                "app.outbox.publisher.enabled",
                () -> "false"
        );

        registry.add(
                "app.outbox.health.enabled",
                () -> "true"
        );

        registry.add(
                "app.outbox.health.max-lag",
                () -> "5m"
        );

        registry.add(
                "vnpay.tmn-code",
                () -> "TEST_TMN"
        );

        registry.add(
                "vnpay.hash-secret",
                () -> "TEST_SECRET"
        );

        registry.add(
                "vnpay.payment-url",
                () ->
                        "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
        );

        registry.add(
                "vnpay.return-url",
                () ->
                        "http://localhost/payment-return"
        );
    }

    @Autowired
    private OutboxEventJpaRepository repository;

    @Autowired
    private OutboxBacklogHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.flush();
    }

    @Test
    void shouldIgnoreUnsupportedPendingEvents() {
        savePending(
                "payment.succeeded",
                LocalDateTime.now(ZoneOffset.UTC)
                        .minusHours(1)
        );

        var health = indicator.health();

        assertThat(health.getStatus())
                .isEqualTo(Status.UP);
    }

    @Test
    void shouldDetectSupportedPendingEvent() {
        savePending(
                "payment.created",
                LocalDateTime.now(ZoneOffset.UTC)
                        .minusHours(1)
        );

        var health = indicator.health();

        assertThat(health.getStatus())
                .isEqualTo(Status.OUT_OF_SERVICE);

        assertThat(health.getDetails())
                .containsEntry("pendingCount", 1L);
    }

    private void savePending(
            String eventType,
            LocalDateTime occurredAt
    ) {
        OutboxEventJpaEntity entity =
                new OutboxEventJpaEntity();

        entity.setId(UUID.randomUUID());
        entity.setAggregateType("PAYMENT");
        entity.setAggregateId(UUID.randomUUID());
        entity.setEventType(eventType);
        entity.setPayload("{}");
        entity.setHeaders(null);
        entity.setOccurredAt(occurredAt);
        entity.setPublishedAt(null);
        entity.setRetryCount(0);
        entity.setNextAttemptAt(null);
        entity.setDeadLetteredAt(null);
        entity.setLastError(null);

        repository.saveAndFlush(entity);
    }
}