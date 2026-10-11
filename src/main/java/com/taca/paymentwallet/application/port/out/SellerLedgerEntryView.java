package com.taca.paymentwallet.application.port.out;

import java.time.Instant;
import java.util.UUID;

public record SellerLedgerEntryView(
        UUID entryId,
        UUID postingId,
        String entryType,
        long amount,
        long balanceAfter,
        String referenceType,
        UUID referenceId,
        String description,
        Instant createdAt
) {
}