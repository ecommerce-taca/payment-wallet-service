package com.taca.paymentwallet.infrastructure.health;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.OutboxMessagePublisherPort;
import com.taca.paymentwallet.infrastructure.messaging.kafka.OutboxHealthProperties;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class OutboxBacklogHealthIndicator
        extends AbstractHealthIndicator {

    private final OutboxEventJpaRepository repository;
    private final OutboxMessagePublisherPort publisherPort;
    private final ClockPort clockPort;
    private final OutboxHealthProperties properties;

    public OutboxBacklogHealthIndicator(
            OutboxEventJpaRepository repository,
            OutboxMessagePublisherPort publisherPort,
            ClockPort clockPort,
            OutboxHealthProperties properties
    ) {
        this.repository = Objects.requireNonNull(repository);

        this.publisherPort = Objects.requireNonNull(publisherPort);

        this.clockPort = Objects.requireNonNull(clockPort);

        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    protected void doHealthCheck(
            Health.Builder builder
    ) {
        if (!properties.enabled()) {
            builder.up().withDetail("enabled", false);

            return;
        }

        Set<String> supportedEventTypes =
                publisherPort.supportedEventTypes();

        if (supportedEventTypes.isEmpty()) {
            builder.up()
                    .withDetail("enabled", true)
                    .withDetail("pendingCount", 0L)
                    .withDetail("reason", "no_supported_event_types");

            return;
        }

        long pendingCount =
                repository.countPendingForHealth(
                        supportedEventTypes
                );

        Optional<LocalDateTime> oldest =
                repository.findOldestPendingOccurredAt(
                        supportedEventTypes
                );

        if (pendingCount == 0 || oldest.isEmpty()) {
            builder.up()
                    .withDetail("enabled", true)
                    .withDetail("pendingCount", 0L)
                    .withDetail(
                            "maxLagSeconds",
                            properties.maxLag().toSeconds()
                    );

            return;
        }

        Instant oldestOccurredAt =
                PersistenceTimeMapper.toInstant(
                        oldest.orElseThrow()
                );

        Duration lag =
                Duration.between(
                        oldestOccurredAt,
                        clockPort.now()
                );

        if (lag.isNegative()) {
            lag = Duration.ZERO;
        }

        if (lag.compareTo(properties.maxLag()) > 0) {
            builder.outOfService();
        } else {
            builder.up();
        }

        builder.withDetail("enabled", true)
                .withDetail(
                        "pendingCount",
                        pendingCount
                )
                .withDetail(
                        "oldestOccurredAt",
                        oldestOccurredAt.toString()
                )
                .withDetail(
                        "lagSeconds",
                        lag.toSeconds()
                )
                .withDetail(
                        "maxLagSeconds",
                        properties.maxLag().toSeconds()
                );
    }
}