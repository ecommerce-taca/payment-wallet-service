package com.taca.paymentwallet.application.gateway.vnpay;

import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;

import java.time.Instant;

public record CreateVnpayPaymentUrlRequest(
        PaymentId paymentId,
        CheckoutGroupId checkoutGroupId,
        Money amount,
        Instant expiresAt,
        String clientIp
) {

    public CreateVnpayPaymentUrlRequest {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null");
        }

        if (checkoutGroupId == null) {
            throw new IllegalArgumentException("checkoutGroupId must not be null");
        }

        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("amount must be positive");
        }

        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt must not be null");
        }

        if (clientIp == null || clientIp.isBlank()) {
            throw new IllegalArgumentException("clientIp must not be blank");
        }

        clientIp = clientIp.trim();
    }
}
