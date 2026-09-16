package com.taca.paymentwallet.application.idempotency;

import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdempotencyScopeTest {

    @Test
    void shouldCreatePaymentScope() {
        UUID checkoutGroupUuid = UUID.randomUUID();
        CheckoutGroupId checkoutGroupId = new CheckoutGroupId(checkoutGroupUuid);

        IdempotencyScope scope = IdempotencyScope.payment(checkoutGroupId);

        assertEquals("PAYMENT:" + checkoutGroupUuid, scope.value());
    }

    @Test
    void shouldCreateRefundScope() {
        UUID paymentUuid = UUID.randomUUID();
        PaymentId paymentId = new PaymentId(paymentUuid);

        IdempotencyScope scope = IdempotencyScope.refund(paymentId);

        assertEquals("REFUND:" + paymentUuid, scope.value());
    }

    @Test
    void shouldCreatePayoutScope() {
        UUID shopUuid = UUID.randomUUID();
        ShopId shopId = new ShopId(shopUuid);

        IdempotencyScope scope = IdempotencyScope.payout(shopId);

        assertEquals("PAYOUT:" + shopUuid, scope.value());
    }

    @Test
    void shouldRejectBlankScope() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new IdempotencyScope(" ")
        );
    }

    @Test
    void shouldRejectNullPaymentScopeId() {
        assertThrows(
                NullPointerException.class,
                () -> IdempotencyScope.payment(null)
        );
    }

    @Test
    void shouldRejectNullRefundScopeId() {
        assertThrows(
                NullPointerException.class,
                () -> IdempotencyScope.refund(null)
        );
    }

    @Test
    void shouldRejectNullPayoutScopeId() {
        assertThrows(
                NullPointerException.class,
                () -> IdempotencyScope.payout(null)
        );
    }
}
