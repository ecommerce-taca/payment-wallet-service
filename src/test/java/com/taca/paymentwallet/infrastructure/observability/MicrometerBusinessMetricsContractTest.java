package com.taca.paymentwallet.infrastructure.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerBusinessMetricsContractTest {

    private final SimpleMeterRegistry registry =
            new SimpleMeterRegistry();

    private final MicrometerBusinessMetricsAdapter metrics =
            new MicrometerBusinessMetricsAdapter(registry);

    @Test
    void shouldKeepAuthoritativeMetricNames() {
        assertThat(MicrometerBusinessMetricsAdapter.PAYMENT_OUTCOME_TOTAL)
                .isEqualTo("payment_wallet_payment_outcome_total");

        assertThat(MicrometerBusinessMetricsAdapter.REFUND_OUTCOME_TOTAL)
                .isEqualTo("payment_wallet_refund_outcome_total");

        assertThat(MicrometerBusinessMetricsAdapter.PAYOUT_OUTCOME_TOTAL)
                .isEqualTo("payment_wallet_payout_outcome_total");

        assertThat(MicrometerBusinessMetricsAdapter.SETTLEMENT_RETRY_TOTAL)
                .isEqualTo("payment_wallet_settlement_retry_total");
    }

    @Test
    void shouldRecordPaymentOutcome() {
        metrics.recordPayment(
                "VNPAY",
                "SUCCESS",
                "APPLIED"
        );

        assertThat(
                registry.get(
                                MicrometerBusinessMetricsAdapter.PAYMENT_OUTCOME_TOTAL
                        )
                        .tags(
                                "method", "vnpay",
                                "status", "success",
                                "action", "applied"
                        )
                        .counter()
                        .count()
        ).isEqualTo(1.0);
    }

    @Test
    void shouldRecordRefundOutcome() {
        metrics.recordRefund("SUCCESS", "APPLIED");

        assertThat(
                registry.get(
                                MicrometerBusinessMetricsAdapter.REFUND_OUTCOME_TOTAL
                        )
                        .tags(
                                "status", "success",
                                "action", "applied"
                        )
                        .counter()
                        .count()
        ).isEqualTo(1.0);
    }

    @Test
    void shouldRecordPayoutOutcome() {
        metrics.recordPayout("FAILED", "APPLIED");

        assertThat(
                registry.get(
                                MicrometerBusinessMetricsAdapter.PAYOUT_OUTCOME_TOTAL
                        )
                        .tags(
                                "status", "failed",
                                "action", "applied"
                        )
                        .counter()
                        .count()
        ).isEqualTo(1.0);
    }

    @Test
    void shouldRecordSettlementRetry() {
        metrics.recordSettlementRetry("COMPLETED");

        assertThat(
                registry.get(
                                MicrometerBusinessMetricsAdapter.SETTLEMENT_RETRY_TOTAL
                        )
                        .tags("status", "completed")
                        .counter()
                        .count()
        ).isEqualTo(1.0);
    }
}