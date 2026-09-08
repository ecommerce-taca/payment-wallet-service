package com.taca.paymentwallet.domain.wallet;

import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;

public class Wallet {

    private final WalletId id;
    private final ShopId shopId;
    private final String currency;
    private Money availableBalance;
    private Money pendingBalance;
    private WalletStatus status;

    private Wallet(
            WalletId id,
            ShopId shopId,
            String currency,
            Money availableBalance,
            Money pendingBalance,
            WalletStatus status
    ) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        if (shopId == null) {
            throw new IllegalArgumentException("shopId must not be null");
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }

        if (availableBalance == null) {
            throw new IllegalArgumentException("availableBalance must not be null");
        }

        if (pendingBalance == null) {
            throw new IllegalArgumentException("pendingBalance must not be null");
        }

        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }

        this.id = id;
        this.shopId = shopId;
        this.currency = currency.toUpperCase();
        this.availableBalance = availableBalance;
        this.pendingBalance = pendingBalance;
        this.status = status;
    }

    public static Wallet create(WalletId id, ShopId shopId) {
        return new Wallet(
                id,
                shopId,
                "VND",
                Money.vnd(0),
                Money.vnd(0),
                WalletStatus.ACTIVE
        );
    }

    public void creditPending(Money amount) {
        requireActive();
        requirePositive(amount);

        this.pendingBalance = this.pendingBalance.add(amount);
    }

    public void releasePendingToAvailable(Money amount) {
        requireActive();
        requirePositive(amount);

        if (pendingBalance.isGreaterThanOrEqual(amount)) {
            this.pendingBalance = this.pendingBalance.subtract(amount);
            this.availableBalance = this.availableBalance.add(amount);
            return;
        }

        throw new InsufficientWalletBalanceException(pendingBalance, amount);
    }

    public void reservePayout(Money amount) {
        requireActive();
        requirePositive(amount);

        if (availableBalance.isGreaterThanOrEqual(amount)) {
            this.availableBalance = this.availableBalance.subtract(amount);
            return;
        }

        throw new InsufficientWalletBalanceException(availableBalance, amount);
    }

    public void reversePayoutReserve(Money amount) {
        requirePositive(amount);

        this.availableBalance = this.availableBalance.add(amount);
    }

    public void freeze() {
        if (status == WalletStatus.CLOSED) {
            throw new IllegalStateException("closed wallet cannot be frozen");
        }

        this.status = WalletStatus.FROZEN;
    }

    public void activate() {
        if (status == WalletStatus.CLOSED) {
            throw new IllegalStateException("closed wallet cannot be activated");
        }

        this.status = WalletStatus.ACTIVE;
    }

    public void close() {
        if (availableBalance.isPositive() || pendingBalance.isPositive()) {
            throw new IllegalStateException("wallet with balance cannot be closed");
        }

        this.status = WalletStatus.CLOSED;
    }

    private void requireActive() {
        if (status != WalletStatus.ACTIVE) {
            throw new IllegalStateException("wallet is not active");
        }
    }

    private void requirePositive(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    public WalletId id() {
        return id;
    }

    public ShopId shopId() {
        return shopId;
    }

    public String currency() {
        return currency;
    }

    public Money availableBalance() {
        return availableBalance;
    }

    public Money pendingBalance() {
        return pendingBalance;
    }

    public WalletStatus status() {
        return status;
    }
}
