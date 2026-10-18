package com.taca.paymentwallet.application.port.out;

import java.util.List;

public record SellerLedgerPage(
        List<SellerLedgerEntryView> entries,
        long total,
        int totalPages
) {

    public SellerLedgerPage {
        entries = List.copyOf(entries);
    }
}