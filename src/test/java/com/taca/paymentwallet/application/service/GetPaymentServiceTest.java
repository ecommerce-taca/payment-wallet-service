package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.exception.PaymentNotFoundException;
import com.taca.paymentwallet.application.port.out.PaymentRepositoryPort;
import com.taca.paymentwallet.application.query.GetPaymentQuery;
import com.taca.paymentwallet.application.result.GetPaymentResult;
import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.payment.PaymentMethod;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.valueobject.BuyerUserId;
import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetPaymentServiceTest {

    @Test
    void shouldReturnPaymentDetail() {
        UUID paymentUuid = UUID.randomUUID();
        UUID checkoutGroupUuid = UUID.randomUUID();
        UUID buyerUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        UUID shopUuid = UUID.randomUUID();

        Payment payment = Payment.create(
                new PaymentId(paymentUuid),
                new CheckoutGroupId(checkoutGroupUuid),
                new BuyerUserId(buyerUuid),
                PaymentMethod.COD,
                Money.vnd(120_000),
                List.of(
                        new PaymentOrder(
                                new OrderId(orderUuid),
                                new ShopId(shopUuid),
                                Money.vnd(100_000),
                                Money.vnd(20_000)
                        )
                )
        );

        GetPaymentService service =
                new GetPaymentService(
                        new FakePaymentRepositoryPort(payment)
                );

        GetPaymentResult result =
                service.execute(new GetPaymentQuery(paymentUuid));

        assertEquals(paymentUuid, result.paymentId());
        assertEquals(checkoutGroupUuid, result.checkoutGroupId());
        assertEquals(buyerUuid, result.buyerUserId());
        assertEquals("PENDING_COD", result.status());
        assertEquals("COD", result.method());
        assertEquals(120_000L, result.amount());
        assertEquals("VND", result.currency());
        assertEquals(0L, result.capturedAmount());
        assertEquals(0L, result.refundedAmount());
        assertNull(result.expiresAt());
        assertNull(result.paidAt());

        assertEquals(1, result.orders().size());
        assertEquals(orderUuid, result.orders().get(0).orderId());
        assertEquals(shopUuid, result.orders().get(0).shopId());
        assertEquals(100_000L, result.orders().get(0).merchandiseAmount());
        assertEquals(20_000L, result.orders().get(0).shippingFee());
        assertEquals(120_000L, result.orders().get(0).amount());
    }

    @Test
    void shouldReturnVnpayExpiry() {
        UUID paymentUuid = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-10-08T15:15:00Z");

        Payment payment = Payment.create(
                new PaymentId(paymentUuid),
                new CheckoutGroupId(UUID.randomUUID()),
                new BuyerUserId(UUID.randomUUID()),
                PaymentMethod.VNPAY,
                Money.vnd(100_000),
                List.of(
                        new PaymentOrder(
                                new OrderId(UUID.randomUUID()),
                                new ShopId(UUID.randomUUID()),
                                Money.vnd(90_000),
                                Money.vnd(10_000)
                        )
                ),
                expiresAt
        );

        GetPaymentService service =
                new GetPaymentService(
                        new FakePaymentRepositoryPort(payment)
                );

        GetPaymentResult result =
                service.execute(new GetPaymentQuery(paymentUuid));

        assertEquals("PENDING", result.status());
        assertEquals("VNPAY", result.method());
        assertEquals(expiresAt, result.expiresAt());
        assertNull(result.paidAt());
    }

    @Test
    void shouldThrowWhenPaymentDoesNotExist() {
        UUID paymentUuid = UUID.randomUUID();

        GetPaymentService service =
                new GetPaymentService(
                        new FakePaymentRepositoryPort(null)
                );

        assertThrows(
                PaymentNotFoundException.class,
                () -> service.execute(new GetPaymentQuery(paymentUuid))
        );
    }

    @Test
    void shouldRejectNullQuery() {
        GetPaymentService service =
                new GetPaymentService(
                        new FakePaymentRepositoryPort(null)
                );

        assertThrows(
                NullPointerException.class,
                () -> service.execute(null)
        );
    }

    private static final class FakePaymentRepositoryPort
            implements PaymentRepositoryPort {

        private final Payment payment;

        private FakePaymentRepositoryPort(Payment payment) {
            this.payment = payment;
        }

        @Override
        public Optional<Payment> findById(PaymentId paymentId) {
            if (payment == null) {
                return Optional.empty();
            }

            return payment.id().equals(paymentId)
                    ? Optional.of(payment)
                    : Optional.empty();
        }

        @Override
        public Optional<Payment> findByCheckoutGroupId(
                CheckoutGroupId checkoutGroupId
        ) {
            return Optional.empty();
        }

        @Override
        public Optional<Payment> findByOrderId(OrderId orderId) {
            return Optional.empty();
        }

        @Override
        public Payment save(Payment payment) {
            throw new UnsupportedOperationException();
        }
    }
}