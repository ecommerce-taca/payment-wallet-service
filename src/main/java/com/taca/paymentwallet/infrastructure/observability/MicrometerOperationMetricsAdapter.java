package com.taca.paymentwallet.infrastructure.observability;

import com.taca.paymentwallet.application.observability.OperationMetric;
import com.taca.paymentwallet.application.observability.OperationResult;
import com.taca.paymentwallet.application.port.out.OperationMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.Objects;

public class MicrometerOperationMetricsAdapter
        implements OperationMetricsPort {

    static final String OPERATION_TOTAL =
            "payment_wallet_operation_total";

    static final String OPERATION_LATENCY =
            "payment_wallet_operation_latency";

    private final MeterRegistry meterRegistry;

    public MicrometerOperationMetricsAdapter(
            MeterRegistry meterRegistry
    ) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry);
    }

    @Override
    public void record(
            OperationMetric operation,
            OperationResult result,
            Duration duration
    ) {
        Objects.requireNonNull(operation, "operation must not be null");
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(duration, "duration must not be null");

        if (duration.isNegative()) {
            throw new IllegalArgumentException(
                    "duration must not be negative"
            );
        }

        meterRegistry.counter(
                OPERATION_TOTAL,
                "operation", operation.tagValue(),
                "result", result.tagValue()
        ).increment();

        Timer.builder(OPERATION_LATENCY)
                .tag("operation", operation.tagValue())
                .tag("result", result.tagValue())
                .register(meterRegistry)
                .record(duration);
    }
}