package com.taca.paymentwallet.domain.wallet;

import com.taca.paymentwallet.domain.valueobject.LedgerPostingId;
import com.taca.paymentwallet.domain.valueobject.Money;

import java.util.List;

public class LedgerPosting {

    private final LedgerPostingId id;
    private final String postingType;
    private final String businessKey;
    private final String referenceType;
    private final String referenceId;
    private final List<LedgerEntry> entries;

    public LedgerPosting(
            LedgerPostingId id,
            String postingType,
            String businessKey,
            String referenceType,
            String referenceId,
            List<LedgerEntry> entries
    ) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        if (postingType == null || postingType.isBlank()) {
            throw new IllegalArgumentException("postingType must not be blank");
        }

        if (businessKey == null || businessKey.isBlank()) {
            throw new IllegalArgumentException("businessKey must not be blank");
        }

        if (referenceType == null || referenceType.isBlank()) {
            throw new IllegalArgumentException("referenceType must not be blank");
        }

        if (referenceId == null || referenceId.isBlank()) {
            throw new IllegalArgumentException("referenceId must not be blank");
        }

        if (entries == null || entries.size() < 2) {
            throw new IllegalArgumentException("ledger posting must have at least two entries");
        }

        this.id = id;
        this.postingType = postingType;
        this.businessKey = businessKey;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.entries = List.copyOf(entries);

        validateBalanced();
    }

    private void validateBalanced() {
        long totalDebit = entries.stream()
                .filter(entry -> entry.entryType() == LedgerEntryType.DEBIT)
                .map(LedgerEntry::amount)
                .mapToLong(Money::amount)
                .sum();

        long totalCredit = entries.stream()
                .filter(entry -> entry.entryType() == LedgerEntryType.CREDIT)
                .map(LedgerEntry::amount)
                .mapToLong(Money::amount)
                .sum();

        if (totalDebit != totalCredit) {
            throw new UnbalancedLedgerPostingException(
                    totalDebit,
                    totalCredit
            );
        }
    }

    public LedgerPostingId id() {
        return id;
    }

    public String postingType() {
        return postingType;
    }

    public String businessKey() {
        return businessKey;
    }

    public String referenceType() {
        return referenceType;
    }

    public String referenceId() {
        return referenceId;
    }

    public List<LedgerEntry> entries() {
        return entries;
    }
}
