package com.taca.paymentwallet.application.command;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreatePaymentCommandTest {

    @Test
    void shouldCreateValidCommand() {
        CreatePaymentOrderCommand order = new CreatePaymentOrderCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                100_000,
                0
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
                90_000,
                0
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
                100_000,
                0
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

    @Test
    void shouldCalculateMerchandiseAmount() {
        CreatePaymentOrderCommand command = new CreatePaymentOrderCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                120_000,
                20_000
        );

        assertThat(command.amount()).isEqualTo(120_000);
        assertThat(command.shippingFee()).isEqualTo(20_000);
        assertThat(command.merchandiseAmount()).isEqualTo(100_000);
    }

    @Test
    void shouldAllowZeroShippingFee() {
        CreatePaymentOrderCommand command = new CreatePaymentOrderCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                100_000,
                0
        );

        assertThat(command.merchandiseAmount()).isEqualTo(100_000);
    }

    @Test
    void shouldRejectNegativeShippingFee() {
        assertThatThrownBy(() -> new CreatePaymentOrderCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                100_000,
                -1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("shippingFee must not be negative");
    }

    @Test
    void shouldRejectShippingFeeEqualToOrderAmount() {
        assertThatThrownBy(() -> new CreatePaymentOrderCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                100_000,
                100_000
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("shippingFee must be less than order amount");
    }

    @Test
    void shouldRejectShippingFeeGreaterThanOrderAmount() {
        assertThatThrownBy(() -> new CreatePaymentOrderCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                100_000,
                120_000
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("shippingFee must be less than order amount");
    }
}