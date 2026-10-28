package com.taca.paymentwallet.infrastructure.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerOperationMetricsContractTest {

    @Test
    void shouldKeepAuthoritativeMetricNames() {
        assertThat(
                MicrometerOperationMetricsAdapter.OPERATION_TOTAL
        ).isEqualTo(
                "payment_wallet_operation_total"
        );

        assertThat(
                MicrometerOperationMetricsAdapter.OPERATION_LATENCY
        ).isEqualTo(
                "payment_wallet_operation_latency"
        );
    }
}