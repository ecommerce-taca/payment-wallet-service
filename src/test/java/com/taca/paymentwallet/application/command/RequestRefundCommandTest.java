package com.taca.paymentwallet.application.command;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestRefundCommandTest {

    @Test
    void shouldNormalizeRefundCommand() {
        UUID paymentId = UUID.randomUUID();

        RequestRefundCommand command =
                new RequestRefundCommand(
                        paymentId,
                        50_000,
                        " vnd ",
                        "  Buyer requested refund  ",
                        "  refund-idem-001  "
                );

        assertEquals(
                paymentId,
                command.paymentId()
        );

        assertEquals(
                50_000L,
                command.amount()
        );

        assertEquals(
                "VND",
                command.currency()
        );

        assertEquals(
                "Buyer requested refund",
                command.reason()
        );

        assertEquals(
                "refund-idem-001",
                command.idempotencyKey()
        );
    }

    @Test
    void shouldRejectNonVndCurrency() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new RequestRefundCommand(
                                UUID.randomUUID(),
                                50_000,
                                "USD",
                                "Buyer requested refund",
                                "refund-idem-001"
                        )
        );
    }

    @Test
    void shouldRejectNonPositiveAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new RequestRefundCommand(
                                UUID.randomUUID(),
                                0,
                                "VND",
                                "Buyer requested refund",
                                "refund-idem-001"
                        )
        );
    }

    @Test
    void shouldRejectBlankReason() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new RequestRefundCommand(
                                UUID.randomUUID(),
                                50_000,
                                "VND",
                                "   ",
                                "refund-idem-001"
                        )
        );
    }

    @Test
    void shouldRejectBlankIdempotencyKey() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new RequestRefundCommand(
                                UUID.randomUUID(),
                                50_000,
                                "VND",
                                "Buyer requested refund",
                                "   "
                        )
        );
    }
}