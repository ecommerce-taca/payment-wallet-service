package com.taca.paymentwallet.application.command;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreatePaymentCommandTest {

    @Test
    void shouldCreateValidCommand() {
        CreatePaymentOrderCommand order = new CreatePaymentOrderCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                100_000
        );

        CreatePaymentCommand command = new CreatePaymentCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "vnpay",
                100_000,
                "vnd",
                " idem-key-1 ",
                List.of(order),
                "127.0.0.1"
        );

        assertEquals("VNPAY", command.method());
        assertEquals("VND", command.currency());
        assertEquals("idem-key-1", command.idempotencyKey());
        assertEquals(1, command.orders().size());
    }

    @Test
    void shouldRejectAmountMismatch() {
        CreatePaymentOrderCommand order = new CreatePaymentOrderCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                90_000
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new CreatePaymentCommand(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "VNPAY",
                        100_000,
                        "VND",
                        "idem-key-1",
                        List.of(order),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void shouldRejectBlankIdempotencyKey() {
        CreatePaymentOrderCommand order = new CreatePaymentOrderCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                100_000
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new CreatePaymentCommand(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "VNPAY",
                        100_000,
                        "VND",
                        " ",
                        List.of(order),
                        "127.0.0.1"
                )
        );
    }
}