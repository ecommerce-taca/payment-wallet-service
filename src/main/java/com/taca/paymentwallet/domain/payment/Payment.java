package com.taca.paymentwallet.domain.payment;

import com.taca.paymentwallet.domain.refund.RefundLimitExceededException;
import com.taca.paymentwallet.domain.valueobject.BuyerUserId;
import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;

import java.time.Instant;
import java.util.List;

public class Payment {

    private final PaymentId id;
    private final CheckoutGroupId checkoutGroupId;
    private final BuyerUserId buyerUserId;
    private final PaymentMethod method;
    private final Money amount;
    private final List<PaymentOrder> orders;
    private PaymentStatus status;
    private Money capturedAmount;
    private Money refundedAmount;
    private Instant paidAt;

    private Payment(
            PaymentId id,
            CheckoutGroupId checkoutGroupId,
            BuyerUserId buyerUserId,
            PaymentMethod method,
            Money amount,
            List<PaymentOrder> orders,
            PaymentStatus status
    ) {
        this.id = id;
        this.checkoutGroupId = checkoutGroupId;
        this.buyerUserId = buyerUserId;
        this.method = method;
        this.amount = amount;
        this.orders = List.copyOf(orders);
        this.status = status;
        this.capturedAmount = Money.vnd(0);
        this.refundedAmount = Money.vnd(0);

        validateTotalOrderAmount();
    }

    public static Payment create(
            PaymentId id,
            CheckoutGroupId checkoutGroupId,
            BuyerUserId buyerUserId,
            PaymentMethod method,
            Money amount,
            List<PaymentOrder> orders
    ) {
        PaymentStatus initialStatus = method == PaymentMethod.COD
                ? PaymentStatus.PENDING_COD
                : PaymentStatus.PENDING;

        return new Payment(
                id,
                checkoutGroupId,
                buyerUserId,
                method,
                amount,
                orders,
                initialStatus);
    }

    public void markSucceeded(Instant paidAt) {
        if (status != PaymentStatus.PENDING && status != PaymentStatus.PENDING_COD) {
            throw new InvalidPaymentStateException(status, "mark succeeded");
        }

        this.status = PaymentStatus.SUCCESS;
        this.capturedAmount = amount;
        this.paidAt = paidAt;
    }

    public void markFailed() {
        if (status != PaymentStatus.PENDING && status != PaymentStatus.PENDING_COD) {
            throw new InvalidPaymentStateException(status, "mark failed");
        }

        this.status = PaymentStatus.FAILED;
    }

    public void markExpired() {
        if (status != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException(status, "mark expired");
        }

        this.status = PaymentStatus.EXPIRED;
    }

    private void validateTotalOrderAmount() {
        long total = orders.stream()
                .map(PaymentOrder::amount)
                .mapToLong(Money::amount)
                .sum();

        if (total != amount.amount()) {
            throw new IllegalArgumentException("total order amount must equal payment amount");
        }
    }

    public PaymentId id() {
        return id;
    }

    public CheckoutGroupId checkoutGroupId() {
        return checkoutGroupId;
    }

    public BuyerUserId buyerUserId() {
        return buyerUserId;
    }

    public PaymentMethod method() {
        return method;
    }

    public Money amount() {
        return amount;
    }

    public List<PaymentOrder> orders() {
        return orders;
    }

    public PaymentStatus status() {
        return status;
    }

    public Money capturedAmount() {
        return capturedAmount;
    }

    public Money refundedAmount() {
        return refundedAmount;
    }

    public Instant paidAt() {
        return paidAt;
    }

    public void validateRefundRequest(
            Money requestedAmount,
            Money processingRefundAmount
    ) {
        if (status != PaymentStatus.SUCCESS
                && status != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new InvalidPaymentStateException(status, "request refund");
        }

        if (requestedAmount == null || !requestedAmount.isPositive()) {
            throw new IllegalArgumentException("requestedAmount must be positive");
        }

        if (processingRefundAmount == null) {
            throw new IllegalArgumentException("processingRefundAmount must not be null");
        }

        Money totalRefundAmount = refundedAmount
                .add(processingRefundAmount)
                .add(requestedAmount);

        if (totalRefundAmount.isGreaterThan(capturedAmount)) {
            throw new RefundLimitExceededException(capturedAmount, totalRefundAmount);
        }
    }

    public void markRefundSucceeded(Money refundAmount) {
        if (status != PaymentStatus.SUCCESS
                && status != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new InvalidPaymentStateException(status, "mark refund succeeded");
        }

        if (refundAmount == null || !refundAmount.isPositive()) {
            throw new IllegalArgumentException("refundAmount must be positive");
        }

        Money newRefundedAmount = refundedAmount.add(refundAmount);

        if (newRefundedAmount.isGreaterThan(capturedAmount)) {
            throw new RefundLimitExceededException(capturedAmount, newRefundedAmount);
        }

        this.refundedAmount = newRefundedAmount;
        this.status = newRefundedAmount.equals(capturedAmount)
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;
    }
}
