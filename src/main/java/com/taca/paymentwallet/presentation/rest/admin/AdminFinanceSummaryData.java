package com.taca.paymentwallet.presentation.rest.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record AdminFinanceSummaryData(
        @JsonProperty("captured_amount")
        long capturedAmount,

        @JsonProperty("refunded_amount")
        long refundedAmount,

        @JsonProperty("gross_amount")
        long grossAmount,

        @JsonProperty("commission_amount")
        long commissionAmount,

        @JsonProperty("tax_amount")
        long taxAmount,

        @JsonProperty("seller_net_amount")
        long sellerNetAmount,

        @JsonProperty("available_balance")
        long availableBalance,

        @JsonProperty("pending_balance")
        long pendingBalance,

        @JsonProperty("completed_payout_amount")
        long completedPayoutAmount,

        @JsonProperty("as_of")
        Instant asOf
) {
}