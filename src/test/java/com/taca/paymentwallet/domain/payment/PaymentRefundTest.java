package com.taca.paymentwallet.domain.payment;

import com.taca.paymentwallet.domain.refund.RefundLimitExceededException;
import com.taca.paymentwallet.domain.valueobject.BuyerUserId;
import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentRefundTest {

    @Test
    void shouldValidateRefundRequestWithinCapturedAmount() {
        Payment payment = createSucceededPayment();

        payment.validateRefundRequest(Money.vnd(40_000), Money.vnd(10_000));
    }

    @Test
    void shouldRejectRefundRequestWhenTotalRefundExceedsCapturedAmount() {
        Payment payment = createSucceededPayment();

        assertThrows(RefundLimitExceededException.class,
                () -> payment.validateRefundRequest(Money.vnd(90_000), Money.vnd(20_000)));
    }

    @Test
    void shouldMarkPaymentPartiallyRefunded() {
        Payment payment = createSucceededPayment();

        payment.markRefundSucceeded(Money.vnd(40_000));

        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, payment.status());
        assertEquals(Money.vnd(40_000), payment.refundedAmount());
    }

    @Test
    void shouldMarkPaymentFullyRefunded() {
        Payment payment = createSucceededPayment();

        payment.markRefundSucceeded(Money.vnd(100_000));

        assertEquals(PaymentStatus.REFUNDED, payment.status());
        assertEquals(Money.vnd(100_000), payment.refundedAmount());
    }

    @Test
    void shouldRejectRefundWhenPaymentIsNotSucceeded() {
        Payment payment = createPendingPayment();

        assertThrows(InvalidPaymentStateException.class,
                () -> payment.validateRefundRequest(Money.vnd(10_000), Money.vnd(0)));
    }

    private Payment createSucceededPayment() {
        Payment payment = createPendingPayment();
        payment.markSucceeded(Instant.now());
        return payment;
    }

    private Payment createPendingPayment() {
        return Payment.create(
                new PaymentId(UUID.randomUUID()),
                new CheckoutGroupId(UUID.randomUUID()),
                new BuyerUserId(UUID.randomUUID()),
                PaymentMethod.VNPAY,
                Money.vnd(100_000),
                List.of(new PaymentOrder(
                        new OrderId(UUID.randomUUID()),
                        new ShopId(UUID.randomUUID()),
                        Money.vnd(100_000)
                ))
        );
    }
}
