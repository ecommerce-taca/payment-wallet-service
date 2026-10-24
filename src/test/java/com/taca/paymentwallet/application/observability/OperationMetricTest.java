package com.taca.paymentwallet.application.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperationMetricTest {

    @Test
    void shouldKeepBoundedOperationValues() {
        assertThat(OperationMetric.values())
                .extracting(OperationMetric::tagValue)
                .containsExactly(
                        "create_payment",
                        "process_vnpay_webhook",
                        "request_refund",
                        "retry_settlement"
                );
    }
}