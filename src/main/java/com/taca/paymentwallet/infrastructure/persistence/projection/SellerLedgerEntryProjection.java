package com.taca.paymentwallet.infrastructure.persistence.projection;

import java.time.LocalDateTime;

public interface SellerLedgerEntryProjection {

    String getEntryId();

    String getPostingId();

    String getEntryType();

    Long getAmount();

    Long getBalanceAfter();

    String getReferenceType();

    String getReferenceId();

    String getDescription();

    LocalDateTime getCreatedAt();
}