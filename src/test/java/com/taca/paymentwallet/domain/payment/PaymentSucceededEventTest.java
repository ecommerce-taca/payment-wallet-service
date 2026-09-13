package com.taca.paymentwallet.domain.payment;

import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentSucceededEventTest {

    @Test
    void shouldCreatePaymentSucceededEvent() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        Money amount = Money.vnd(100_000);

        PaymentSucceededEvent event = PaymentSucceededEvent.now(paymentId, amount);

        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.paymentId()).isEqualTo(paymentId);
        assertThat(event.capturedAmount()).isEqualTo(amount);
        assertThat(event.aggregateId()).isEqualTo(paymentId.value().toString());
        assertThat(event.eventType()).isEqualTo("payment.succeeded");
    }
}
