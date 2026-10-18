package com.taca.paymentwallet.presentation.rest.seller;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record SellerWalletData(
        @JsonProperty("wallet_id")
        UUID walletId,

        @JsonProperty("shop_id")
        UUID shopId,

        @JsonProperty("available_balance")
        long availableBalance,

        @JsonProperty("pending_balance")
        long pendingBalance,

        String currency,

        String status,

        @JsonProperty("as_of")
        Instant asOf
) {
}