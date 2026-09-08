package com.taca.paymentwallet.domain.wallet;

import com.taca.paymentwallet.domain.valueobject.LedgerAccountId;
import com.taca.paymentwallet.domain.valueobject.ShopId;

public class LedgerAccount {

    private final LedgerAccountId id;
    private final String accountCode;
    private final LedgerAccountType accountType;
    private final String ownerType;
    private final ShopId ownerId;
    private final String currency;

    public LedgerAccount(
            LedgerAccountId id,
            String accountCode,
            LedgerAccountType accountType,
            String ownerType,
            ShopId ownerId,
            String currency
    ) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        if (accountCode == null || accountCode.isBlank()) {
            throw new IllegalArgumentException("accountCode must not be blank");
        }

        if (accountType == null) {
            throw new IllegalArgumentException("accountType must not be null");
        }

        if (ownerType == null || ownerType.isBlank()) {
            throw new IllegalArgumentException("ownerType must not be blank");
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }

        this.id = id;
        this.accountCode = accountCode;
        this.accountType = accountType;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.currency = currency.toUpperCase();
    }

    public static LedgerAccount system(
            LedgerAccountId id,
            String accountCode,
            LedgerAccountType accountType
    ) {
        return new LedgerAccount(
                id,
                accountCode,
                accountType,
                "SYSTEM",
                null,
                "VND"
        );
    }

    public static LedgerAccount seller(
            LedgerAccountId id,
            String accountCode,
            LedgerAccountType accountType,
            ShopId shopId
    ) {
        if (accountType != LedgerAccountType.SELLER_PENDING
                && accountType != LedgerAccountType.SELLER_AVAILABLE) {
            throw new IllegalArgumentException("seller account type must be SELLER_PENDING or SELLER_AVAILABLE");
        }

        if (shopId == null) {
            throw new IllegalArgumentException("shopId must not be null");
        }

        return new LedgerAccount(
                id,
                accountCode,
                accountType,
                "SHOP", shopId,
                "VND"
        );
    }

    public LedgerAccountId id() {
        return id;
    }

    public String accountCode() {
        return accountCode;
    }

    public LedgerAccountType accountType() {
        return accountType;
    }

    public String ownerType() {
        return ownerType;
    }

    public ShopId ownerId() {
        return ownerId;
    }

    public String currency() {
        return currency;
    }
}
