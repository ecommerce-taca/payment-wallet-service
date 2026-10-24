package com.taca.paymentwallet.presentation.rest.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record AdminFinanceSummaryExportData(
        @JsonProperty("download_url") String downloadUrl,
        @JsonProperty("expires_at") Instant expiresAt
) {
}