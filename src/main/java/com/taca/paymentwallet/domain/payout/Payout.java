package com.taca.paymentwallet.domain.payout;

import com.taca.paymentwallet.domain.valueobject.IdempotencyKey;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PayoutId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;

public class Payout {

    private final PayoutId id;
    private final WalletId walletId;
    private final ShopId shopId;
    private final Money amount;
    private final BankAccountSnapshot bankAccountSnapshot;
    private final IdempotencyKey idempotencyKey;
    private PayoutStatus status;
    private String providerRef;
    private String failureCode;

    private Payout(
            PayoutId id,
            WalletId walletId,
            ShopId shopId,
            Money amount,
            BankAccountSnapshot bankAccountSnapshot,
            IdempotencyKey idempotencyKey,
            PayoutStatus status
    ) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        if (walletId == null) {
            throw new IllegalArgumentException("walletId must not be null");
        }

        if (shopId == null) {
            throw new IllegalArgumentException("shopId must not be null");
        }

        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("amount must be positive");
        }

        if (bankAccountSnapshot == null) {
            throw new IllegalArgumentException("bankAccountSnapshot must not be null");
        }

        if (idempotencyKey == null) {
            throw new IllegalArgumentException("idempotencyKey must not be null");
        }

        if (status == null) throw new IllegalArgumentException("status must not be null");

        this.id = id;
        this.walletId = walletId;
        this.shopId = shopId;
        this.amount = amount;
        this.bankAccountSnapshot = bankAccountSnapshot;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
    }

    public static Payout request(
            PayoutId id,
            WalletId walletId,
            ShopId shopId,
            Money amount,
            BankAccountSnapshot bankAccountSnapshot,
            IdempotencyKey idempotencyKey
    ) {
        return new Payout(
                id,
                walletId,
                shopId,
                amount,
                bankAccountSnapshot,
                idempotencyKey,
                PayoutStatus.REQUESTED
        );
    }

    public void markProcessing() {
        if (status != PayoutStatus.REQUESTED) {
            throw new InvalidPayoutStateException(status, "mark processing");
        }

        this.status = PayoutStatus.PROCESSING;
    }

    public void markSucceeded(String providerRef) {
        if (status != PayoutStatus.PROCESSING) {
            throw new InvalidPayoutStateException(status, "mark succeeded");
        }

        if (providerRef == null || providerRef.isBlank()) {
            throw new IllegalArgumentException("providerRef must not be blank");
        }

        this.providerRef = providerRef;
        this.status = PayoutStatus.SUCCESS;
    }

    public void markFailed(String failureCode) {
        if (status != PayoutStatus.PROCESSING) {
            throw new InvalidPayoutStateException(status, "mark failed");
        }

        if (failureCode == null || failureCode.isBlank()) {
            throw new IllegalArgumentException("failureCode must not be blank");
        }

        this.failureCode = failureCode;
        this.status = PayoutStatus.FAILED;
    }

    public void cancel() {
        if (status != PayoutStatus.REQUESTED) {
            throw new InvalidPayoutStateException(status, "cancel");
        }

        this.status = PayoutStatus.CANCELLED;
    }

    public PayoutId id() {
        return id;
    }

    public WalletId walletId() {
        return walletId;
    }

    public ShopId shopId() {
        return shopId;
    }

    public Money amount() {
        return amount;
    }

    public BankAccountSnapshot bankAccountSnapshot() {
        return bankAccountSnapshot;
    }

    public IdempotencyKey idempotencyKey() {
        return idempotencyKey;
    }

    public PayoutStatus status() {
        return status;
    }

    public String providerRef() {
        return providerRef;
    }

    public String failureCode() {
        return failureCode;
    }
}