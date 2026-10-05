package com.taca.paymentwallet.infrastructure.health;

import com.taca.paymentwallet.application.outbox.OutboxMessage;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.OutboxMessagePublisherPort;
import com.taca.paymentwallet.infrastructure.messaging.kafka.OutboxHealthProperties;
import com.taca.paymentwallet.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OutboxBacklogHealthIndicatorTest {

    private static final Instant NOW =
            Instant.parse("2026-10-04T10:00:00Z");

    private final OutboxEventJpaRepository repository =
            mock(OutboxEventJpaRepository.class);

    private final OutboxMessagePublisherPort publisherPort =
            mock(OutboxMessagePublisherPort.class);

    private final ClockPort clockPort =
            () -> NOW;

    @Test
    void shouldBeUpWhenThereIsNoPendingOutboxEvent() {
        when(publisherPort.supportedEventTypes())
                .thenReturn(Set.of(
                        "payment.created",
                        "wallet.allocated"
                ));

        when(repository.countPendingForHealth(anySet()))
                .thenReturn(0L);

        when(repository.findOldestPendingOccurredAt(anySet()))
                .thenReturn(Optional.empty());

        var indicator = indicator(Duration.ofMinutes(5));

        var health = indicator.health();

        assertThat(health.getStatus())
                .isEqualTo(Status.UP);

        assertThat(health.getDetails())
                .containsEntry("pendingCount", 0L);
    }

    @Test
    void shouldBeUpWhenOldestPendingEventIsWithinThreshold() {
        when(publisherPort.supportedEventTypes())
                .thenReturn(Set.of(
                        "payment.created",
                        "wallet.allocated"
                ));

        when(repository.countPendingForHealth(anySet()))
                .thenReturn(3L);

        when(repository.findOldestPendingOccurredAt(anySet()))
                .thenReturn(Optional.of(
                        localDateTime(
                                NOW.minusSeconds(120)
                        )
                ));

        var health =
                indicator(Duration.ofMinutes(5))
                        .health();

        assertThat(health.getStatus())
                .isEqualTo(Status.UP);

        assertThat(health.getDetails())
                .containsEntry("pendingCount", 3L)
                .containsEntry("lagSeconds", 120L)
                .containsEntry("maxLagSeconds", 300L);
    }

    @Test
    void shouldBeOutOfServiceWhenOutboxLagExceedsThreshold() {
        when(publisherPort.supportedEventTypes())
                .thenReturn(Set.of(
                        "payment.created",
                        "wallet.allocated"
                ));

        when(repository.countPendingForHealth(anySet()))
                .thenReturn(4L);

        when(repository.findOldestPendingOccurredAt(anySet()))
                .thenReturn(Optional.of(
                        localDateTime(
                                NOW.minusSeconds(601)
                        )
                ));

        var health =
                indicator(Duration.ofMinutes(5))
                        .health();

        assertThat(health.getStatus())
                .isEqualTo(Status.OUT_OF_SERVICE);

        assertThat(health.getDetails())
                .containsEntry("pendingCount", 4L)
                .containsEntry("lagSeconds", 601L);
    }

    @Test
    void shouldIgnoreHealthGuardWhenDisabled() {
        OutboxBacklogHealthIndicator indicator =
                new OutboxBacklogHealthIndicator(
                        repository,
                        publisherPort,
                        clockPort,
                        new OutboxHealthProperties(
                                false,
                                Duration.ofMinutes(5)
                        )
                );

        var health = indicator.health();

        assertThat(health.getStatus())
                .isEqualTo(Status.UP);

        assertThat(health.getDetails())
                .containsEntry("enabled", false);

        verifyNoInteractions(
                repository,
                publisherPort
        );
    }

    @Test
    void shouldOnlyQuerySupportedEventTypes() {
        Set<String> eventTypes =
                Set.of(
                        "payment.created",
                        "wallet.allocated"
                );

        when(publisherPort.supportedEventTypes())
                .thenReturn(eventTypes);

        when(repository.countPendingForHealth(eventTypes))
                .thenReturn(0L);

        when(repository.findOldestPendingOccurredAt(eventTypes))
                .thenReturn(Optional.empty());

        indicator(Duration.ofMinutes(5))
                .health();

        verify(repository)
                .countPendingForHealth(eventTypes);

        verify(repository)
                .findOldestPendingOccurredAt(eventTypes);
    }

    private OutboxBacklogHealthIndicator indicator(
            Duration maxLag
    ) {
        return new OutboxBacklogHealthIndicator(
                repository,
                publisherPort,
                clockPort,
                new OutboxHealthProperties(
                        true,
                        maxLag
                )
        );
    }

    private LocalDateTime localDateTime(
            Instant instant
    ) {
        return LocalDateTime.ofInstant(
                instant,
                ZoneOffset.UTC
        );
    }
}