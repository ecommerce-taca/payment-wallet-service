package com.taca.paymentwallet.presentation.rest.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record RequestRefundData(
        @JsonProperty("refund_id")
        UUID refundId,

        @JsonProperty("payment_id")
        UUID paymentId,

        long amount,

        String currency,

        String status
) {
}