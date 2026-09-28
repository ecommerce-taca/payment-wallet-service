package com.taca.paymentwallet.presentation.rest.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.Map;

public record VnpayWebhookRequest(

        @JsonProperty("provider_event_id")
        @NotBlank
        String providerEventId,

        @JsonProperty("provider_transaction_ref")
        @NotBlank
        String providerTransactionRef,

        @JsonProperty("response_code")
        @NotBlank
        String responseCode,

        @JsonProperty("transaction_status")
        @NotBlank
        String transactionStatus,

        @Positive
        long amount,

        @NotBlank
        String currency,

        @JsonProperty("payload_hash")
        @NotBlank
        String payloadHash,

        @JsonProperty("signed_payload")
        @NotEmpty
        Map<String, String> signedPayload
) {
}