package com.taca.paymentwallet.domain.payment;

import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.ShopId;

import java.time.Instant;

public record PaymentOrder(
        OrderId orderId,
        ShopId shopId,
        Money merchandiseAmount,
        Money shippingFee,
        PaymentOrderCodStatus codStatus,
        Instant codProcessedAt,
        String codFailureCode
) {

    public PaymentOrder {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }

        if (shopId == null) {
            throw new IllegalArgumentException("shopId must not be null");
        }

        if (merchandiseAmount == null || !merchandiseAmount.isPositive()) {
            throw new IllegalArgumentException("merchandiseAmount must be positive");
        }

        if (shippingFee == null) {
            throw new IllegalArgumentException("shippingFee must not be null");
        }

        if (!merchandiseAmount.currency().equals(shippingFee.currency())) {
            throw new IllegalArgumentException(
                    "merchandiseAmount and shippingFee must use the same currency"
            );
        }

        if (codStatus == null) {
            throw new IllegalArgumentException("codStatus must not be null");
        }

        if (codStatus == PaymentOrderCodStatus.PENDING
                && (codProcessedAt != null || codFailureCode != null)) {
            throw new IllegalArgumentException(
                    "PENDING COD order must not contain processing result"
            );
        }

        if (codStatus == PaymentOrderCodStatus.CAPTURED
                && (codProcessedAt == null || codFailureCode != null)) {
            throw new IllegalArgumentException(
                    "CAPTURED COD order requires processedAt and no failureCode"
            );
        }

        if (codStatus == PaymentOrderCodStatus.FAILED
                && (codProcessedAt == null || codFailureCode == null || codFailureCode.isBlank())) {
            throw new IllegalArgumentException(
                    "FAILED COD order requires processedAt and failureCode"
            );
        }

        codFailureCode = codFailureCode == null ? null : codFailureCode.trim().toUpperCase();
    }

    public PaymentOrder(OrderId orderId, ShopId shopId, Money merchandiseAmount, Money shippingFee) {
        this(
                orderId,
                shopId,
                merchandiseAmount,
                shippingFee,
                PaymentOrderCodStatus.PENDING,
                null,
                null
        );
    }

    public PaymentOrder(OrderId orderId, ShopId shopId, Money merchandiseAmount) {
        this(orderId, shopId, merchandiseAmount, new Money(0, merchandiseAmount.currency()));
    }

    public Money totalAmount() {
        return merchandiseAmount.add(shippingFee);
    }

    public Money amount() {
        return totalAmount();
    }

    public boolean isCodPending() {
        return codStatus == PaymentOrderCodStatus.PENDING;
    }

    public boolean isCodCaptured() {
        return codStatus == PaymentOrderCodStatus.CAPTURED;
    }

    public boolean isCodFailed() {
        return codStatus == PaymentOrderCodStatus.FAILED;
    }

    public PaymentOrder captureCod(Instant occurredAt) {
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }

        if (!isCodPending()) {
            throw new IllegalStateException("COD order is already terminal: " + codStatus);
        }

        return new PaymentOrder(
                orderId,
                shopId,
                merchandiseAmount,
                shippingFee,
                PaymentOrderCodStatus.CAPTURED,
                occurredAt,
                null
        );
    }

    public PaymentOrder failCod(Instant occurredAt, String failureCode) {
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }

        if (failureCode == null || failureCode.isBlank()) {
            throw new IllegalArgumentException("failureCode must not be blank");
        }

        if (!isCodPending()) {
            throw new IllegalStateException("COD order is already terminal: " + codStatus);
        }

        return new PaymentOrder(
                orderId,
                shopId,
                merchandiseAmount,
                shippingFee,
                PaymentOrderCodStatus.FAILED,
                occurredAt,
                failureCode
        );
    }
}