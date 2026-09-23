package com.taca.paymentwallet.domain.payment;

import com.taca.paymentwallet.domain.AggregateRoot;
import com.taca.paymentwallet.domain.refund.RefundLimitExceededException;
import com.taca.paymentwallet.domain.valueobject.BuyerUserId;
import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;

import java.time.Instant;
import java.util.List;

public class Payment extends AggregateRoot {

    private final PaymentId id;
    private final CheckoutGroupId checkoutGroupId;
    private final BuyerUserId buyerUserId;
    private final PaymentMethod method;
    private final Money amount;
    private final List<PaymentOrder> orders;
    private PaymentStatus status;
    private String failureCode;
    private Money capturedAmount;
    private Money refundedAmount;
    private Instant expiresAt;
    private Instant paidAt;

    private Payment(
            PaymentId id,
            CheckoutGroupId checkoutGroupId,
            BuyerUserId buyerUserId,
            PaymentMethod method,
            Money amount,
            List<PaymentOrder> orders,
            PaymentStatus status,
            Money capturedAmount,
            Money refundedAmount,
            String failureCode,
            Instant expiresAt,
            Instant paidAt
    ) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        if (checkoutGroupId == null) {
            throw new IllegalArgumentException("checkoutGroupId must not be null");
        }

        if (buyerUserId == null) {
            throw new IllegalArgumentException("buyerUserId must not be null");
        }

        if (method == null) {
            throw new IllegalArgumentException("method must not be null");
        }

        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("amount must be positive");
        }

        if (orders == null || orders.isEmpty()) {
            throw new IllegalArgumentException("orders must not be empty");
        }

        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }

        if (capturedAmount == null) {
            throw new IllegalArgumentException("capturedAmount must not be null");
        }

        if (refundedAmount == null) {
            throw new IllegalArgumentException("refundedAmount must not be null");
        }

        if (refundedAmount.isGreaterThan(capturedAmount)) {
            throw new IllegalArgumentException(
                    "refundedAmount must not be greater than capturedAmount"
            );
        }

        this.id = id;
        this.checkoutGroupId = checkoutGroupId;
        this.buyerUserId = buyerUserId;
        this.method = method;
        this.amount = amount;
        this.orders = List.copyOf(orders);
        this.status = status;
        this.capturedAmount = capturedAmount;
        this.refundedAmount = refundedAmount;
        this.failureCode = failureCode;
        this.expiresAt = expiresAt;
        this.paidAt = paidAt;

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
        if (method == PaymentMethod.VNPAY) {
            throw new IllegalArgumentException(
                    "VNPAY payment requires expiresAt"
            );
        }

        return create(
                id,
                checkoutGroupId,
                buyerUserId,
                method,
                amount,
                orders,
                null
        );
    }

    public static Payment create(
            PaymentId id,
            CheckoutGroupId checkoutGroupId,
            BuyerUserId buyerUserId,
            PaymentMethod method,
            Money amount,
            List<PaymentOrder> orders,
            Instant expiresAt
    ) {
        PaymentStatus initialStatus = method == PaymentMethod.COD
                ? PaymentStatus.PENDING_COD
                : PaymentStatus.PENDING;

        if (method == PaymentMethod.VNPAY && expiresAt == null) {
            throw new IllegalArgumentException(
                    "expiresAt must not be null for VNPAY payment"
            );
        }

        if (method == PaymentMethod.COD && expiresAt != null) {
            throw new IllegalArgumentException(
                    "expiresAt must be null for COD payment"
            );
        }

        return new Payment(
                id,
                checkoutGroupId,
                buyerUserId,
                method,
                amount,
                orders,
                initialStatus,
                Money.vnd(0),
                Money.vnd(0),
                null,
                expiresAt,
                null
        );
    }

    public static Payment rehydrate(
            PaymentId id,
            CheckoutGroupId checkoutGroupId,
            BuyerUserId buyerUserId,
            PaymentMethod method,
            Money amount,
            List<PaymentOrder> orders,
            PaymentStatus status,
            Money capturedAmount,
            Money refundedAmount,
            String failureCode,
            Instant expiresAt,
            Instant paidAt
    ) {
        return new Payment(
                id,
                checkoutGroupId,
                buyerUserId,
                method,
                amount,
                orders,
                status,
                capturedAmount,
                refundedAmount,
                failureCode,
                expiresAt,
                paidAt
        );
    }

    public void markSucceeded(Instant paidAt) {
        if (status != PaymentStatus.PENDING && status != PaymentStatus.PENDING_COD) {
            throw new InvalidPaymentStateException(status, "mark succeeded");
        }

        if (paidAt == null) {
            throw new IllegalArgumentException("paidAt must not be null");
        }

        this.status = PaymentStatus.SUCCESS;
        this.capturedAmount = amount;
        this.paidAt = paidAt;

        registerEvent(PaymentSucceededEvent.now(id, capturedAmount));
    }

    public void markFailed(String failureCode) {
        if (status != PaymentStatus.PENDING && status != PaymentStatus.PENDING_COD) {
            throw new InvalidPaymentStateException(status, "mark failed");
        }

        if (failureCode == null || failureCode.isBlank()) {
            throw new IllegalArgumentException("Failure code must not be blank");
        }

        this.status = PaymentStatus.FAILED;
        this.failureCode = failureCode;

        registerEvent(PaymentFailedEvent.now(id, failureCode));
    }

    public void markExpired() {
        if (status != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException(status, "mark expired");
        }

        this.status = PaymentStatus.EXPIRED;

        registerEvent(PaymentExpiredEvent.now(id));
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

    public String failureCode() {
        return failureCode;
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

    public Instant expiresAt() {
        return expiresAt;
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

        registerEvent(PaymentRefundedEvent.now(id, refundAmount, status));
    }
}
