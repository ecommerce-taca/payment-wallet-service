package com.taca.paymentwallet.domain.payment;

import com.taca.paymentwallet.domain.AggregateRoot;
import com.taca.paymentwallet.domain.refund.RefundLimitExceededException;
import com.taca.paymentwallet.domain.valueobject.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        this.orders = new ArrayList<>(orders);
        this.status = status;
        this.capturedAmount = capturedAmount;
        this.refundedAmount = refundedAmount;
        this.failureCode = failureCode;
        this.expiresAt = expiresAt;
        this.paidAt = paidAt;

        validateTotalOrderAmount();
    }

    public PaymentOrder order(OrderId orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }

        return orders.stream()
                .filter(order -> order.orderId().equals(orderId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "order does not belong to payment: " + orderId.value()
                        ));
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

        Payment payment =
                new Payment(
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

        payment.registerEvent(
                new PaymentCreatedEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        payment.id(),
                        payment.checkoutGroupId(),
                        payment.orders()
                                .stream()
                                .map(PaymentOrder::orderId)
                                .toList(),
                        payment.amount(),
                        payment.method(),
                        payment.status()
                )
        );

        return payment;
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
        Money total = orders.stream()
                .map(PaymentOrder::totalAmount)
                .reduce(
                        Money.vnd(0),
                        Money::add
                );

        if (!total.equals(amount)) {
            throw new IllegalArgumentException(
                    "total order amount must equal payment amount"
            );
        }
    }

    public PaymentOrder captureCodOrder(
            OrderId orderId,
            Instant occurredAt
    ) {
        ensurePendingCod();

        PaymentOrder current = order(orderId);

        if (!current.isCodPending()) {
            return current;
        }

        PaymentOrder captured = current.captureCod(occurredAt);
        replaceOrder(captured);

        capturedAmount = capturedAmount.add(captured.totalAmount());

        if (capturedAmount.isGreaterThan(amount)) {
            throw new IllegalStateException("captured amount exceeds payment amount");
        }

        if (allCodOrdersCaptured()) {
            markSucceeded(occurredAt);
        }

        return captured;
    }

    public PaymentOrder failCodOrder(
            OrderId orderId,
            Instant occurredAt,
            String failureCode
    ) {
        ensurePendingCod();

        PaymentOrder current = order(orderId);

        if (!current.isCodPending()) {
            return current;
        }

        PaymentOrder failed = current.failCod(occurredAt, failureCode);
        replaceOrder(failed);

        if (allCodOrdersFailed() && !capturedAmount.isPositive()) {
            markFailed(failed.codFailureCode());
        }

        return failed;
    }

    private void replaceOrder(PaymentOrder updatedOrder) {
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i).orderId().equals(updatedOrder.orderId())) {
                orders.set(i, updatedOrder);
                return;
            }
        }

        throw new IllegalArgumentException(
                "order does not belong to payment: " + updatedOrder.orderId().value()
        );
    }

    private boolean allCodOrdersCaptured() {
        return orders.stream().allMatch(PaymentOrder::isCodCaptured);
    }

    private boolean allCodOrdersFailed() {
        return orders.stream().allMatch(PaymentOrder::isCodFailed);
    }

    private void ensurePendingCod() {
        if (method != PaymentMethod.COD || status != PaymentStatus.PENDING_COD) {
            throw new InvalidPaymentStateException(status, "process COD order");
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
        return List.copyOf(orders);
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
