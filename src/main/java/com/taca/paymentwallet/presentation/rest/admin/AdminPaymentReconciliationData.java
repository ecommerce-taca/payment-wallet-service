package com.taca.paymentwallet.presentation.rest.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record AdminPaymentReconciliationData(
        @JsonProperty("payment_id")
        UUID paymentId,

        String provider,

        @JsonProperty("local_status")
        String localStatus,

        @JsonProperty("provider_status")
        String providerStatus,

        long amount,

        @JsonProperty("match")
        boolean matched,

        @JsonProperty("checked_at")
        Instant checkedAt
) {
}