package com.taca.paymentwallet.application.result;

import com.taca.paymentwallet.application.payment.CodPaymentProcessingAction;

import java.util.UUID;

public record ProcessCodPaymentResult(
        UUID paymentId,
        UUID checkoutGroupId,
        UUID orderId,
        String paymentStatus,
        String orderCodStatus,
        CodPaymentProcessingAction action
) {

    public ProcessCodPaymentResult {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null");
        }

        if (checkoutGroupId == null) {
            throw new IllegalArgumentException("checkoutGroupId must not be null");
        }

        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }

        if (paymentStatus == null || paymentStatus.isBlank()) {
            throw new IllegalArgumentException("paymentStatus must not be blank");
        }

        if (orderCodStatus == null || orderCodStatus.isBlank()) {
            throw new IllegalArgumentException("orderCodStatus must not be blank");
        }

        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }

        paymentStatus = paymentStatus.trim().toUpperCase();
        orderCodStatus = orderCodStatus.trim().toUpperCase();
    }
}