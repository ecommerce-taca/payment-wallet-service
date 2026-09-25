package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.exception.LedgerAccountNotFoundException;
import com.taca.paymentwallet.application.exception.PaymentAllocationNotFoundException;
import com.taca.paymentwallet.application.port.out.LedgerAccountLookupPort;
import com.taca.paymentwallet.domain.valueobject.LedgerAccountId;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.wallet.LedgerAccountType;
import com.taca.paymentwallet.infrastructure.persistence.entity.LedgerAccountJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentAllocationJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.LedgerAccountJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentAllocationJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.SettlementLineJpaRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LedgerAccountLookupAdapter
        implements LedgerAccountLookupPort {

    private static final String CURRENCY = "VND";
    private static final String SYSTEM_OWNER = "SYSTEM";
    private static final String SHOP_OWNER = "SHOP";
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final LedgerAccountJpaRepository ledgerAccountRepository;
    private final PaymentAllocationJpaRepository paymentAllocationRepository;
    private final SettlementLineJpaRepository settlementLineRepository;

    public LedgerAccountLookupAdapter(
            LedgerAccountJpaRepository ledgerAccountRepository,
            PaymentAllocationJpaRepository paymentAllocationRepository,
            SettlementLineJpaRepository settlementLineRepository
    ) {
        this.ledgerAccountRepository =
                Objects.requireNonNull(
                        ledgerAccountRepository
                );

        this.paymentAllocationRepository =
                Objects.requireNonNull(
                        paymentAllocationRepository
                );

        this.settlementLineRepository =
                Objects.requireNonNull(
                        settlementLineRepository
                );
    }

    @Override
    public LedgerAccountId vnpayClearingAccount() {
        return systemAccount(
                LedgerAccountType.VNPAY_CLEARING
        );
    }

    @Override
    public LedgerAccountId codClearingAccount() {
        return systemAccount(
                LedgerAccountType.COD_CLEARING
        );
    }

    @Override
    public LedgerAccountId platformCommissionAccount() {
        return systemAccount(
                LedgerAccountType.PLATFORM_COMMISSION
        );
    }

    @Override
    public LedgerAccountId taxPayableAccount() {
        return systemAccount(
                LedgerAccountType.TAX_PAYABLE
        );
    }

    @Override
    public LedgerAccountId refundClearingAccount() {
        return systemAccount(
                LedgerAccountType.REFUND_CLEARING
        );
    }

    @Override
    public LedgerAccountId payoutClearingAccount() {
        return systemAccount(
                LedgerAccountType.PAYOUT_CLEARING
        );
    }

    @Override
    public Map<ShopId, LedgerAccountId> sellerPendingAccountsFor(
            List<ShopId> shopIds
    ) {
        Objects.requireNonNull(
                shopIds,
                "shopIds must not be null"
        );

        Map<ShopId, LedgerAccountId> result =
                new LinkedHashMap<>();

        for (ShopId shopId : shopIds) {
            Objects.requireNonNull(
                    shopId,
                    "shopId must not be null"
            );

            if (result.containsKey(shopId)) {
                continue;
            }

            result.put(
                    shopId,
                    sellerAccount(
                            shopId,
                            LedgerAccountType.SELLER_PENDING
                    )
            );
        }

        return result;
    }

    @Override
    public LedgerAccountId sellerAvailableAccount(
            ShopId shopId
    ) {
        Objects.requireNonNull(
                shopId,
                "shopId must not be null"
        );

        return sellerAccount(
                shopId,
                LedgerAccountType.SELLER_AVAILABLE
        );
    }

    @Override
    public Map<PaymentAllocationId, LedgerAccountId>
    sellerRefundAccountsFor(
            List<PaymentAllocationId> paymentAllocationIds
    ) {
        Objects.requireNonNull(
                paymentAllocationIds,
                "paymentAllocationIds must not be null"
        );

        Map<PaymentAllocationId, LedgerAccountId> result =
                new LinkedHashMap<>();

        for (PaymentAllocationId allocationId
                : paymentAllocationIds) {

            Objects.requireNonNull(
                    allocationId,
                    "paymentAllocationId must not be null"
            );

            if (result.containsKey(allocationId)) {
                continue;
            }

            PaymentAllocationJpaEntity allocation =
                    paymentAllocationRepository
                            .findById(
                                    allocationId.value()
                            )
                            .orElseThrow(
                                    () ->
                                            new PaymentAllocationNotFoundException(
                                                    allocationId
                                            )
                            );

            ShopId shopId =
                    new ShopId(
                            allocation.getShopId()
                    );

            LedgerAccountType accountType =
                    settlementLineRepository
                            .existsByPaymentAllocationId(
                                    allocationId.value()
                            )
                            ? LedgerAccountType.SELLER_AVAILABLE
                            : LedgerAccountType.SELLER_PENDING;

            result.put(
                    allocationId,
                    sellerAccount(
                            shopId,
                            accountType
                    )
            );
        }

        return result;
    }

    private LedgerAccountId systemAccount(
            LedgerAccountType accountType
    ) {
        String accountCode =
                accountType.name()
                        + ":"
                        + CURRENCY;

        LedgerAccountJpaEntity entity =
                ledgerAccountRepository
                        .findByAccountCodeAndCurrency(
                                accountCode,
                                CURRENCY
                        )
                        .orElseThrow(
                                () ->
                                        new LedgerAccountNotFoundException(
                                                accountCode
                                        )
                        );

        ensureActive(
                entity,
                accountCode
        );

        ensureSystemAccount(
                entity,
                accountType
        );

        return new LedgerAccountId(
                entity.getId()
        );
    }

    private LedgerAccountId sellerAccount(
            ShopId shopId,
            LedgerAccountType accountType
    ) {
        LedgerAccountJpaEntity entity =
                ledgerAccountRepository
                        .findByOwnerTypeAndOwnerIdAndAccountTypeAndCurrency(
                                SHOP_OWNER,
                                shopId.value(),
                                accountType.name(),
                                CURRENCY
                        )
                        .orElseThrow(
                                () ->
                                        new LedgerAccountNotFoundException(
                                                accountType.name()
                                                        + " for shop "
                                                        + shopId.value()
                                        )
                        );

        ensureActive(
                entity,
                accountType.name()
                        + " for shop "
                        + shopId.value()
        );

        return new LedgerAccountId(
                entity.getId()
        );
    }

    private void ensureActive(
            LedgerAccountJpaEntity entity,
            String description
    ) {
        if (!ACTIVE_STATUS.equals(
                entity.getStatus()
        )) {
            throw new LedgerAccountNotFoundException(
                    description
                            + " is not active"
            );
        }
    }

    private void ensureSystemAccount(
            LedgerAccountJpaEntity entity,
            LedgerAccountType expectedType
    ) {
        if (!SYSTEM_OWNER.equals(
                entity.getOwnerType()
        )) {
            throw new IllegalStateException(
                    "system ledger account has invalid owner type: "
                            + entity.getAccountCode()
            );
        }

        if (entity.getOwnerId() != null) {
            throw new IllegalStateException(
                    "system ledger account must not have owner id: "
                            + entity.getAccountCode()
            );
        }

        if (!expectedType.name().equals(
                entity.getAccountType()
        )) {
            throw new IllegalStateException(
                    "ledger account type mismatch: "
                            + entity.getAccountCode()
            );
        }
    }
}