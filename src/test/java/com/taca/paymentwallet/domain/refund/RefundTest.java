package com.taca.paymentwallet.domain.refund;

import com.taca.paymentwallet.domain.valueobject.IdempotencyKey;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.RefundId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefundTest {

    @Test
    void shouldCreateRequestedRefund() {
        Refund refund = createRefund();

        assertEquals(RefundStatus.REQUESTED, refund.status());
    }

    @Test
    void shouldMarkRefundProcessing() {
        Refund refund = createRefund();

        refund.markProcessing();

        assertEquals(RefundStatus.PROCESSING, refund.status());
    }

    @Test
    void shouldMarkRefundSucceededFromProcessing() {
        Refund refund = createRefund();

        refund.markProcessing();
        refund.markSucceeded();

        assertEquals(RefundStatus.SUCCESS, refund.status());
    }

    @Test
    void shouldRejectCancelAfterProcessing() {
        Refund refund = createRefund();

        refund.markProcessing();

        assertThrows(InvalidRefundStateException.class, refund::cancel);
    }

    private Refund createRefund() {
        return Refund.request(
                new RefundId(UUID.randomUUID()),
                new PaymentId(UUID.randomUUID()),
                Money.vnd(50_000),
                "Customer returned item",
                new IdempotencyKey(UUID.randomUUID().toString())
        );
    }
}
