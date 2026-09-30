package com.taca.paymentwallet.presentation.rest.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreatePaymentData(

        @JsonProperty("payment_id")
        UUID paymentId,

        @JsonProperty("checkout_group_id")
        UUID checkoutGroupId,

        String status,

        String method,

        long amount,

        String currency,

        @JsonProperty("payment_url")
        String paymentUrl,

        @JsonProperty("expires_at")
        Instant expiresAt
) {
}