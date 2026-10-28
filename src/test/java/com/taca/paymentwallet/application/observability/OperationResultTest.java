package com.taca.paymentwallet.application.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperationResultTest {

    @Test
    void shouldKeepBoundedResultValues() {
        assertThat(OperationResult.values())
                .extracting(OperationResult::tagValue)
                .containsExactly(
                        "success",
                        "failure",
                        "duplicate",
                        "rejected"
                );
    }
}