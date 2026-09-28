package com.taca.paymentwallet.presentation.rest.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record VnpayWebhookData(

        @JsonProperty("payment_id")
        UUID paymentId,

        @JsonProperty("payment_status")
        String paymentStatus,

        String action
) {
}