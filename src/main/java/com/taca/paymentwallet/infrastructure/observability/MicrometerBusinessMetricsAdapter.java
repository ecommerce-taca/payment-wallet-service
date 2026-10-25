package com.taca.paymentwallet.infrastructure.observability;

import com.taca.paymentwallet.application.port.out.BusinessMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Objects;

public class MicrometerBusinessMetricsAdapter
        implements BusinessMetricsPort {

    static final String PAYMENT_OUTCOME_TOTAL =
            "payment_wallet_payment_outcome_total";

    static final String REFUND_OUTCOME_TOTAL =
            "payment_wallet_refund_outcome_total";

    static final String PAYOUT_OUTCOME_TOTAL =
            "payment_wallet_payout_outcome_total";

    static final String SETTLEMENT_RETRY_TOTAL =
            "payment_wallet_settlement_retry_total";

    private final MeterRegistry meterRegistry;

    public MicrometerBusinessMetricsAdapter(
            MeterRegistry meterRegistry
    ) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry);
    }

    @Override
    public void recordPayment(
            String method,
            String status,
            String action
    ) {
        meterRegistry.counter(
                PAYMENT_OUTCOME_TOTAL,
                "method", normalize(method),
                "status", normalize(status),
                "action", normalize(action)
        ).increment();
    }

    @Override
    public void recordRefund(
            String status,
            String action
    ) {
        meterRegistry.counter(
                REFUND_OUTCOME_TOTAL,
                "status", normalize(status),
                "action", normalize(action)
        ).increment();
    }

    @Override
    public void recordPayout(
            String status,
            String action
    ) {
        meterRegistry.counter(
                PAYOUT_OUTCOME_TOTAL,
                "status", normalize(status),
                "action", normalize(action)
        ).increment();
    }

    @Override
    public void recordSettlementRetry(String status) {
        meterRegistry.counter(
                SETTLEMENT_RETRY_TOTAL,
                "status", normalize(status)
        ).increment();
    }

    private String normalize(String value) {
        return Objects.requireNonNull(value)
                .trim()
                .toLowerCase();
    }
}