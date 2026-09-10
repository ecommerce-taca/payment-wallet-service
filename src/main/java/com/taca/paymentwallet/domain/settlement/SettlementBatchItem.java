package com.taca.paymentwallet.domain.settlement;

import com.taca.paymentwallet.domain.valueobject.LedgerPostingId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.SettlementBatchItemId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;

import java.util.List;

public class SettlementBatchItem {

    private final SettlementBatchItemId id;
    private final ShopId shopId;
    private final WalletId walletId;
    private final Money gross;
    private final Money commission;
    private final Money tax;
    private final Money net;
    private final Money releasedAmount;
    private final Money heldAmount;
    private final List<SettlementLine> lines;
    private SettlementBatchItemStatus status;
    private LedgerPostingId postingId;

    public SettlementBatchItem(
            SettlementBatchItemId id,
            ShopId shopId,
            WalletId walletId,
            Money gross,
            Money commission,
            Money tax,
            Money net,
            Money releasedAmount,
            Money heldAmount,
            List<SettlementLine> lines
    ) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        if (shopId == null) {
            throw new IllegalArgumentException("shopId must not be null");
        }

        if (walletId == null) {
            throw new IllegalArgumentException("walletId must not be null");
        }

        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("lines must not be empty");
        }

        this.id = id;
        this.shopId = shopId;
        this.walletId = walletId;
        this.gross = requireMoney(gross, "gross");
        this.commission = requireMoney(commission, "commission");
        this.tax = requireMoney(tax, "tax");
        this.net = requireMoney(net, "net");
        this.releasedAmount = requireMoney(releasedAmount, "releasedAmount");
        this.heldAmount = requireMoney(heldAmount, "heldAmount");
        this.lines = List.copyOf(lines);
        this.status = SettlementBatchItemStatus.PENDING;

        validateAmounts();
    }

    public void markCompleted(LedgerPostingId postingId) {
        if (status != SettlementBatchItemStatus.PENDING) {
            throw new InvalidSettlementStateException("Only pending item can be completed");
        }
        if (postingId == null) {
            throw new IllegalArgumentException("postingId must not be null");
        }

        this.postingId = postingId;
        this.status = SettlementBatchItemStatus.COMPLETED;
    }

    public void markFailed() {
        if (status != SettlementBatchItemStatus.PENDING) {
            throw new InvalidSettlementStateException("Only pending item can be failed");
        }

        this.status = SettlementBatchItemStatus.FAILED;
    }

    private void validateAmounts() {
        if (!commission.add(tax).add(net).equals(gross)) {
            throw new IllegalArgumentException("gross must equal commission + tax + net");
        }

        if (!releasedAmount.add(heldAmount).equals(net)) {
            throw new IllegalArgumentException("net must equal releasedAmount + heldAmount");
        }

        Money totalLineAmount = lines.stream()
                .map(SettlementLine::releasedAmount)
                .reduce(Money.vnd(0), Money::add);

        if (!totalLineAmount.equals(releasedAmount)) {
            throw new IllegalArgumentException("total settlement line amount must equal releasedAmount");
        }
    }

    private Money requireMoney(Money money, String fieldName) {
        if (money == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return money;
    }

    public SettlementBatchItemId id() {
        return id;
    }

    public ShopId shopId() {
        return shopId;
    }

    public WalletId walletId() {
        return walletId;
    }

    public Money gross() {
        return gross;
    }

    public Money commission() {
        return commission;
    }

    public Money tax() {
        return tax;
    }

    public Money net() {
        return net;
    }

    public Money releasedAmount() {
        return releasedAmount;
    }

    public Money heldAmount() {
        return heldAmount;
    }

    public List<SettlementLine> lines() {
        return lines;
    }

    public SettlementBatchItemStatus status() {
        return status;
    }

    public LedgerPostingId postingId() {
        return postingId;
    }
}
