package com.taca.paymentwallet.domain.payment;

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

class PaymentTest {

    @Test
    void shouldCreateVnpayPaymentWithPendingStatus() {
        Payment payment = createPayment(PaymentMethod.VNPAY);

        assertEquals(PaymentStatus.PENDING, payment.status());
    }

    @Test
    void shouldCreateCodPaymentWithPendingCodStatus() {
        Payment payment = createPayment(PaymentMethod.COD);

        assertEquals(PaymentStatus.PENDING_COD, payment.status());
    }

    @Test
    void shouldMarkPaymentSucceeded() {
        Payment payment = createPayment(PaymentMethod.VNPAY);
        Instant paidAt = Instant.now();

        payment.markSucceeded(paidAt);

        assertEquals(PaymentStatus.SUCCESS, payment.status());
        assertEquals(Money.vnd(100_000), payment.capturedAmount());
        assertEquals(paidAt, payment.paidAt());
    }

    @Test
    void shouldRejectInvalidOrderTotal() {
        assertThrows(IllegalArgumentException.class, () -> Payment.create(
                new PaymentId(UUID.randomUUID()),
                new CheckoutGroupId(UUID.randomUUID()),
                new BuyerUserId(UUID.randomUUID()),
                PaymentMethod.VNPAY,
                Money.vnd(100_000),
                List.of(new PaymentOrder(
                        new OrderId(UUID.randomUUID()),
                        new ShopId(UUID.randomUUID()),
                        Money.vnd(90_000)
                ))
        ));
    }

    @Test
    void shouldNotExpireCodPayment() {
        Payment payment = createPayment(PaymentMethod.COD);

        assertThrows(InvalidPaymentStateException.class, payment::markExpired);
    }

    private Payment createPayment(PaymentMethod method) {
        return Payment.create(
                new PaymentId(UUID.randomUUID()),
                new CheckoutGroupId(UUID.randomUUID()),
                new BuyerUserId(UUID.randomUUID()),
                method,
                Money.vnd(100_000),
                List.of(new PaymentOrder(
                        new OrderId(UUID.randomUUID()),
                        new ShopId(UUID.randomUUID()),
                        Money.vnd(100_000)
                ))
        );
    }
}
