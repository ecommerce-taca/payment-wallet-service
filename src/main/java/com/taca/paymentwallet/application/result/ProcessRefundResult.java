package com.taca.paymentwallet.application.result;

import com.taca.paymentwallet.application.refund.RefundResultProcessingAction;

import java.util.UUID;

public record ProcessRefundResult(
        UUID refundId,
        UUID paymentId,
        String refundStatus,
        String paymentStatus,
        RefundResultProcessingAction action
) {

    public ProcessRefundResult {
        if (refundId == null) {
            throw new IllegalArgumentException("refundId must not be null");
        }

        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null");
        }

        if (refundStatus == null || refundStatus.isBlank()) {
            throw new IllegalArgumentException("refundStatus must not be blank");
        }

        if (paymentStatus == null || paymentStatus.isBlank()) {
            throw new IllegalArgumentException("paymentStatus must not be blank");
        }
        
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }

        refundStatus = refundStatus.trim().toUpperCase();
        paymentStatus = paymentStatus.trim().toUpperCase();
    }
}