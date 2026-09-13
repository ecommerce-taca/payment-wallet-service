package com.taca.paymentwallet.application.gateway.vnpay;

import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateVnpayPaymentUrlRequestTest {

    @Test
    void shouldCreateValidRequest() {
        CreateVnpayPaymentUrlRequest request = new CreateVnpayPaymentUrlRequest(
                new PaymentId(UUID.randomUUID()),
                new CheckoutGroupId(UUID.randomUUID()),
                Money.vnd(100_000),
                Instant.now().plusSeconds(900),
                " 127.0.0.1 "
        );

        assertEquals("127.0.0.1", request.clientIp());
    }

    @Test
    void shouldRejectBlankClientIp() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CreateVnpayPaymentUrlRequest(
                        new PaymentId(UUID.randomUUID()),
                        new CheckoutGroupId(UUID.randomUUID()),
                        Money.vnd(100_000),
                        Instant.now().plusSeconds(900),
                        " "
                )
        );
    }
}