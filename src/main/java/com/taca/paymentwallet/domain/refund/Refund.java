package com.taca.paymentwallet.domain.refund;

import com.taca.paymentwallet.domain.AggregateRoot;
import com.taca.paymentwallet.domain.valueobject.IdempotencyKey;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.RefundId;

public class Refund extends AggregateRoot {

    private final RefundId id;
    private final PaymentId paymentId;
    private final Money amount;
    private final String reason;
    private final IdempotencyKey idempotencyKey;
    private RefundStatus status;

    private Refund(
            RefundId id,
            PaymentId paymentId,
            Money amount,
            String reason,
            IdempotencyKey idempotencyKey,
            RefundStatus status
    ) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null");
        }

        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("amount must be positive");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }

        if (idempotencyKey == null) {
            throw new IllegalArgumentException("idempotencyKey must not be null");
        }

        if (status == null) throw new IllegalArgumentException("status must not be null");

        this.id = id;
        this.paymentId = paymentId;
        this.amount = amount;
        this.reason = reason;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
    }

    public static Refund request(
            RefundId id,
            PaymentId paymentId,
            Money amount,
            String reason,
            IdempotencyKey idempotencyKey
    ) {
        return new Refund(
                id,
                paymentId,
                amount,
                reason,
                idempotencyKey,
                RefundStatus.REQUESTED);
    }

    public void markProcessing() {
        if (status != RefundStatus.REQUESTED) {
            throw new InvalidRefundStateException(status, "mark processing");
        }

        this.status = RefundStatus.PROCESSING;
    }

    public void markSucceeded() {
        if (status != RefundStatus.REQUESTED
                && status != RefundStatus.PROCESSING) {
            throw new InvalidRefundStateException(status, "mark succeeded");
        }

        this.status = RefundStatus.SUCCESS;

        registerEvent(RefundSucceededEvent.now(id, paymentId, amount));
    }

    public void markFailed() {
        if (status != RefundStatus.REQUESTED
                && status != RefundStatus.PROCESSING) {
            throw new InvalidRefundStateException(status, "mark failed");
        }

        this.status = RefundStatus.FAILED;
    }

    public void cancel() {
        if (status != RefundStatus.REQUESTED) {
            throw new InvalidRefundStateException(status, "cancel");
        }

        this.status = RefundStatus.CANCELLED;
    }

    public RefundId id() {
        return id;
    }

    public PaymentId paymentId() {
        return paymentId;
    }

    public Money amount() {
        return amount;
    }

    public String reason() {
        return reason;
    }

    public IdempotencyKey idempotencyKey() {
        return idempotencyKey;
    }

    public RefundStatus status() {
        return status;
    }
}
