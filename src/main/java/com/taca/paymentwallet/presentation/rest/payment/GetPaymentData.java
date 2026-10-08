package com.taca.paymentwallet.presentation.rest.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GetPaymentData(
        @JsonProperty("payment_id")
        UUID paymentId,

        @JsonProperty("checkout_group_id")
        UUID checkoutGroupId,

        String status,

        String method,

        long amount,

        String currency,

        @JsonProperty("captured_amount")
        long capturedAmount,

        @JsonProperty("refunded_amount")
        long refundedAmount,

        @JsonProperty("expires_at")
        Instant expiresAt,

        @JsonProperty("paid_at")
        Instant paidAt,

        List<GetPaymentOrderData> orders
) {
}