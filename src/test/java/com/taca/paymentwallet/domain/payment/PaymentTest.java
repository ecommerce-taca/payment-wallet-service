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

import static org.junit.jupiter.api.Assertions.assertTrue;
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
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Payment.create(
                        new PaymentId(UUID.randomUUID()),
                        new CheckoutGroupId(UUID.randomUUID()),
                        new BuyerUserId(UUID.randomUUID()),
                        PaymentMethod.VNPAY,
                        Money.vnd(100_000),
                        List.of(new PaymentOrder(
                                new OrderId(UUID.randomUUID()),
                                new ShopId(UUID.randomUUID()),
                                Money.vnd(90_000)
                        )),
                        Instant.parse("2026-01-01T00:15:00Z")
                )
        );

        assertEquals(
                "total order amount must equal payment amount",
                exception.getMessage()
        );
    }

    @Test
    void shouldNotExpireCodPayment() {
        Payment payment = createPayment(PaymentMethod.COD);

        assertThrows(InvalidPaymentStateException.class, payment::markExpired);
    }

    private Payment createPayment(PaymentMethod method) {
        Instant expiresAt = method == PaymentMethod.VNPAY
                ? Instant.parse("2026-01-01T00:15:00Z")
                : null;

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
                )),
                expiresAt
        );
    }

    @Test
    void shouldRehydratePersistedPaymentWithoutPublishingDomainEvent() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        CheckoutGroupId checkoutGroupId =
                new CheckoutGroupId(UUID.randomUUID());
        BuyerUserId buyerUserId =
                new BuyerUserId(UUID.randomUUID());

        OrderId orderId = new OrderId(UUID.randomUUID());
        ShopId shopId = new ShopId(UUID.randomUUID());

        Instant expiresAt =
                Instant.parse("2026-09-23T12:15:00Z");

        Instant paidAt =
                Instant.parse("2026-09-23T12:05:00Z");

        Payment payment = Payment.rehydrate(
                paymentId,
                checkoutGroupId,
                buyerUserId,
                PaymentMethod.VNPAY,
                Money.vnd(100_000),
                List.of(new PaymentOrder(
                        orderId,
                        shopId,
                        Money.vnd(100_000)
                )),
                PaymentStatus.SUCCESS,
                Money.vnd(100_000),
                Money.vnd(0),
                null,
                expiresAt,
                paidAt
        );

        assertEquals(paymentId, payment.id());
        assertEquals(checkoutGroupId, payment.checkoutGroupId());
        assertEquals(buyerUserId, payment.buyerUserId());
        assertEquals(PaymentMethod.VNPAY, payment.method());
        assertEquals(PaymentStatus.SUCCESS, payment.status());

        assertEquals(Money.vnd(100_000), payment.amount());
        assertEquals(Money.vnd(100_000), payment.capturedAmount());
        assertEquals(Money.vnd(0), payment.refundedAmount());

        assertEquals(expiresAt, payment.expiresAt());
        assertEquals(paidAt, payment.paidAt());

        assertTrue(payment.domainEvents().isEmpty());
    }

    @Test
    void shouldRejectVnpayPaymentWithoutExpiresAt() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Payment.create(
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
                )
        );

        assertEquals(
                "VNPAY payment requires expiresAt",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectCodPaymentWithExpiresAt() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Payment.create(
                        new PaymentId(UUID.randomUUID()),
                        new CheckoutGroupId(UUID.randomUUID()),
                        new BuyerUserId(UUID.randomUUID()),
                        PaymentMethod.COD,
                        Money.vnd(100_000),
                        List.of(new PaymentOrder(
                                new OrderId(UUID.randomUUID()),
                                new ShopId(UUID.randomUUID()),
                                Money.vnd(100_000)
                        )),
                        Instant.parse("2026-01-01T00:15:00Z")
                )
        );

        assertEquals(
                "expiresAt must be null for COD payment",
                exception.getMessage()
        );
    }
}
