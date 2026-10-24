package com.taca.paymentwallet.infrastructure.observability;

import com.taca.paymentwallet.application.observability.OperationMetric;
import com.taca.paymentwallet.application.observability.OperationResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MicrometerOperationMetricsAdapterTest {

    private SimpleMeterRegistry registry;
    private MicrometerOperationMetricsAdapter adapter;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        adapter = new MicrometerOperationMetricsAdapter(registry);
    }

    @Test
    void shouldRecordOperationCounter() {
        adapter.record(
                OperationMetric.CREATE_PAYMENT,
                OperationResult.SUCCESS,
                Duration.ofMillis(25)
        );

        assertThat(
                registry.get(
                                MicrometerOperationMetricsAdapter.OPERATION_TOTAL
                        )
                        .tags(
                                "operation",
                                "create_payment",
                                "result",
                                "success"
                        )
                        .counter()
                        .count()
        ).isEqualTo(1.0);
    }

    @Test
    void shouldRecordOperationLatency() {
        adapter.record(
                OperationMetric.REQUEST_REFUND,
                OperationResult.FAILURE,
                Duration.ofMillis(40)
        );

        assertThat(
                registry.get(
                                MicrometerOperationMetricsAdapter.OPERATION_LATENCY
                        )
                        .tags(
                                "operation",
                                "request_refund",
                                "result",
                                "failure"
                        )
                        .timer()
                        .count()
        ).isEqualTo(1L);

        assertThat(
                registry.get(
                                MicrometerOperationMetricsAdapter.OPERATION_LATENCY
                        )
                        .tags(
                                "operation",
                                "request_refund",
                                "result",
                                "failure"
                        )
                        .timer()
                        .totalTime(
                                java.util.concurrent.TimeUnit.MILLISECONDS
                        )
        ).isEqualTo(40.0);
    }

    @Test
    void shouldKeepOperationsSeparatedByTags() {
        adapter.record(
                OperationMetric.CREATE_PAYMENT,
                OperationResult.SUCCESS,
                Duration.ofMillis(10)
        );

        adapter.record(
                OperationMetric.CREATE_PAYMENT,
                OperationResult.FAILURE,
                Duration.ofMillis(20)
        );

        assertThat(
                registry.get(
                                MicrometerOperationMetricsAdapter.OPERATION_TOTAL
                        )
                        .tags(
                                "operation",
                                "create_payment",
                                "result",
                                "success"
                        )
                        .counter()
                        .count()
        ).isEqualTo(1.0);

        assertThat(
                registry.get(
                                MicrometerOperationMetricsAdapter.OPERATION_TOTAL
                        )
                        .tags(
                                "operation",
                                "create_payment",
                                "result",
                                "failure"
                        )
                        .counter()
                        .count()
        ).isEqualTo(1.0);
    }

    @Test
    void shouldAccumulateRepeatedOperations() {
        adapter.record(
                OperationMetric.RETRY_SETTLEMENT,
                OperationResult.SUCCESS,
                Duration.ofMillis(10)
        );

        adapter.record(
                OperationMetric.RETRY_SETTLEMENT,
                OperationResult.SUCCESS,
                Duration.ofMillis(20)
        );

        assertThat(
                registry.get(
                                MicrometerOperationMetricsAdapter.OPERATION_TOTAL
                        )
                        .tags(
                                "operation",
                                "retry_settlement",
                                "result",
                                "success"
                        )
                        .counter()
                        .count()
        ).isEqualTo(2.0);

        assertThat(
                registry.get(
                                MicrometerOperationMetricsAdapter.OPERATION_LATENCY
                        )
                        .tags(
                                "operation",
                                "retry_settlement",
                                "result",
                                "success"
                        )
                        .timer()
                        .count()
        ).isEqualTo(2L);
    }

    @Test
    void shouldRejectNegativeDuration() {
        assertThatThrownBy(() ->
                adapter.record(
                        OperationMetric.CREATE_PAYMENT,
                        OperationResult.SUCCESS,
                        Duration.ofMillis(-1)
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("duration must not be negative");
    }

    @Test
    void shouldRejectNullOperation() {
        assertThatThrownBy(() ->
                adapter.record(
                        null,
                        OperationResult.SUCCESS,
                        Duration.ZERO
                )
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage("operation must not be null");
    }

    @Test
    void shouldRejectNullResult() {
        assertThatThrownBy(() ->
                adapter.record(
                        OperationMetric.CREATE_PAYMENT,
                        null,
                        Duration.ZERO
                )
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage("result must not be null");
    }

    @Test
    void shouldRejectNullDuration() {
        assertThatThrownBy(() ->
                adapter.record(
                        OperationMetric.CREATE_PAYMENT,
                        OperationResult.SUCCESS,
                        null
                )
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage("duration must not be null");
    }
}