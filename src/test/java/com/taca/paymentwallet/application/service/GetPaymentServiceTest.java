package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.exception.ForbiddenException;
import com.taca.paymentwallet.application.exception.PaymentNotFoundException;
import com.taca.paymentwallet.application.port.out.PaymentRepositoryPort;
import com.taca.paymentwallet.application.query.GetPaymentQuery;
import com.taca.paymentwallet.application.result.GetPaymentResult;
import com.taca.paymentwallet.application.security.PaymentVisibilityPolicy;
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
import static org.mockito.Mockito.*;

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

        PaymentVisibilityPolicy visibilityPolicy =
                mock(PaymentVisibilityPolicy.class);

        GetPaymentService service =
                new GetPaymentService(
                        new FakePaymentRepositoryPort(payment),
                        visibilityPolicy
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

        verify(visibilityPolicy).requireCanView(result);
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

        PaymentVisibilityPolicy visibilityPolicy =
                mock(PaymentVisibilityPolicy.class);

        GetPaymentService service =
                new GetPaymentService(
                        new FakePaymentRepositoryPort(payment),
                        visibilityPolicy
                );

        GetPaymentResult result =
                service.execute(new GetPaymentQuery(paymentUuid));

        assertEquals("PENDING", result.status());
        assertEquals("VNPAY", result.method());
        assertEquals(expiresAt, result.expiresAt());
        assertNull(result.paidAt());

        verify(visibilityPolicy).requireCanView(result);
    }

    @Test
    void shouldThrowWhenPaymentDoesNotExist() {
        UUID paymentUuid = UUID.randomUUID();

        PaymentVisibilityPolicy visibilityPolicy =
                mock(PaymentVisibilityPolicy.class);

        GetPaymentService service =
                new GetPaymentService(
                        new FakePaymentRepositoryPort(null),
                        visibilityPolicy
                );

        assertThrows(
                PaymentNotFoundException.class,
                () -> service.execute(new GetPaymentQuery(paymentUuid))
        );

        verifyNoInteractions(visibilityPolicy);
    }

    @Test
    void shouldRejectNullQuery() {
        PaymentVisibilityPolicy visibilityPolicy =
                mock(PaymentVisibilityPolicy.class);

        GetPaymentService service =
                new GetPaymentService(
                        new FakePaymentRepositoryPort(null),
                        visibilityPolicy
                );

        assertThrows(
                NullPointerException.class,
                () -> service.execute(null)
        );

        verifyNoInteractions(visibilityPolicy);
    }

    @Test
    void shouldRejectPaymentWhenVisibilityPolicyDeniesAccess() {
        UUID paymentUuid = UUID.randomUUID();

        Payment payment =
                Payment.create(
                        new PaymentId(paymentUuid),
                        new CheckoutGroupId(UUID.randomUUID()),
                        new BuyerUserId(UUID.randomUUID()),
                        PaymentMethod.COD,
                        Money.vnd(100_000),
                        List.of(
                                new PaymentOrder(
                                        new OrderId(UUID.randomUUID()),
                                        new ShopId(UUID.randomUUID()),
                                        Money.vnd(90_000),
                                        Money.vnd(10_000)
                                )
                        )
                );

        PaymentVisibilityPolicy visibilityPolicy =
                mock(PaymentVisibilityPolicy.class);

        doThrow(new ForbiddenException())
                .when(visibilityPolicy)
                .requireCanView(any());

        GetPaymentService service =
                new GetPaymentService(
                        new FakePaymentRepositoryPort(payment),
                        visibilityPolicy
                );

        assertThrows(
                ForbiddenException.class,
                () -> service.execute(new GetPaymentQuery(paymentUuid))
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