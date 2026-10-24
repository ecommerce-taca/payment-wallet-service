package com.taca.paymentwallet.application.port.out;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AdminPaymentReconciliationViewTest {

    @Test
    void shouldNormalizeStatuses() {
        AdminPaymentReconciliationView view =
                new AdminPaymentReconciliationView(
                        UUID.randomUUID(),
                        " vnpay ",
                        " success ",
                        " success ",
                        1_000_000L,
                        true,
                        Instant.parse("2026-10-21T12:00:00Z")
                );

        assertEquals("VNPAY", view.provider());
        assertEquals("SUCCESS", view.localStatus());
        assertEquals("SUCCESS", view.providerStatus());
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AdminPaymentReconciliationView(
                        UUID.randomUUID(),
                        "VNPAY",
                        "SUCCESS",
                        "SUCCESS",
                        -1L,
                        false,
                        Instant.parse("2026-10-21T12:00:00Z")
                )
        );
    }
}