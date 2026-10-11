package com.taca.paymentwallet.presentation.rest.seller;

import java.util.List;
import java.util.Objects;

public record SellerLedgerResponse(
        List<SellerLedgerData> data,
        SellerLedgerMeta meta
) {

    public SellerLedgerResponse {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(meta, "meta must not be null");
        data = List.copyOf(data);
    }
}