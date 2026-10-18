package com.taca.paymentwallet.application.query;

import java.util.UUID;

public record GetSellerWalletQuery(UUID shopId) {

    public GetSellerWalletQuery {
        if (shopId == null) {
            throw new IllegalArgumentException(
                    "shopId must not be null"
            );
        }
    }
}