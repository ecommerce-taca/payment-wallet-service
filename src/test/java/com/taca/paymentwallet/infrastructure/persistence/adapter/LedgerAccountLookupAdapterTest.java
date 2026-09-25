package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.exception.LedgerAccountNotFoundException;
import com.taca.paymentwallet.application.exception.PaymentAllocationNotFoundException;
import com.taca.paymentwallet.domain.valueobject.LedgerAccountId;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.wallet.LedgerAccountType;
import com.taca.paymentwallet.infrastructure.persistence.entity.LedgerAccountJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentAllocationJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.LedgerAccountJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentAllocationJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.SettlementLineJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LedgerAccountLookupAdapterTest {

    private LedgerAccountJpaRepository ledgerAccountRepository;
    private PaymentAllocationJpaRepository paymentAllocationRepository;
    private SettlementLineJpaRepository settlementLineRepository;

    private LedgerAccountLookupAdapter adapter;

    @BeforeEach
    void setUp() {
        ledgerAccountRepository =
                mock(LedgerAccountJpaRepository.class);

        paymentAllocationRepository =
                mock(PaymentAllocationJpaRepository.class);

        settlementLineRepository =
                mock(SettlementLineJpaRepository.class);

        adapter =
                new LedgerAccountLookupAdapter(
                        ledgerAccountRepository,
                        paymentAllocationRepository,
                        settlementLineRepository
                );
    }

    @Test
    void shouldFindVnpayClearingAccount() {
        UUID accountId =
                UUID.randomUUID();

        when(
                ledgerAccountRepository
                        .findByAccountCodeAndCurrency(
                                "VNPAY_CLEARING:VND",
                                "VND"
                        )
        ).thenReturn(
                Optional.of(
                        systemAccount(
                                accountId,
                                LedgerAccountType.VNPAY_CLEARING
                        )
                )
        );

        assertEquals(
                new LedgerAccountId(accountId),
                adapter.vnpayClearingAccount()
        );
    }

    @Test
    void shouldFindAllSystemAccounts() {
        assertSystemAccount(
                LedgerAccountType.VNPAY_CLEARING,
                adapter::vnpayClearingAccount
        );

        assertSystemAccount(
                LedgerAccountType.COD_CLEARING,
                adapter::codClearingAccount
        );

        assertSystemAccount(
                LedgerAccountType.PLATFORM_COMMISSION,
                adapter::platformCommissionAccount
        );

        assertSystemAccount(
                LedgerAccountType.TAX_PAYABLE,
                adapter::taxPayableAccount
        );

        assertSystemAccount(
                LedgerAccountType.REFUND_CLEARING,
                adapter::refundClearingAccount
        );

        assertSystemAccount(
                LedgerAccountType.PAYOUT_CLEARING,
                adapter::payoutClearingAccount
        );
    }

    @Test
    void shouldFindSellerPendingAccounts() {
        ShopId firstShop =
                new ShopId(UUID.randomUUID());

        ShopId secondShop =
                new ShopId(UUID.randomUUID());

        UUID firstAccountId =
                UUID.randomUUID();

        UUID secondAccountId =
                UUID.randomUUID();

        whenSellerAccount(
                firstShop,
                LedgerAccountType.SELLER_PENDING,
                firstAccountId
        );

        whenSellerAccount(
                secondShop,
                LedgerAccountType.SELLER_PENDING,
                secondAccountId
        );

        Map<ShopId, LedgerAccountId> result =
                adapter.sellerPendingAccountsFor(
                        List.of(
                                firstShop,
                                secondShop
                        )
                );

        assertEquals(
                2,
                result.size()
        );

        assertEquals(
                new LedgerAccountId(firstAccountId),
                result.get(firstShop)
        );

        assertEquals(
                new LedgerAccountId(secondAccountId),
                result.get(secondShop)
        );
    }

    @Test
    void shouldNotQueryDuplicateSellerPendingAccountTwice() {
        ShopId shopId =
                new ShopId(UUID.randomUUID());

        whenSellerAccount(
                shopId,
                LedgerAccountType.SELLER_PENDING,
                UUID.randomUUID()
        );

        Map<ShopId, LedgerAccountId> result =
                adapter.sellerPendingAccountsFor(
                        List.of(
                                shopId,
                                shopId
                        )
                );

        assertEquals(
                1,
                result.size()
        );

        verify(
                ledgerAccountRepository,
                times(1)
        ).findByOwnerTypeAndOwnerIdAndAccountTypeAndCurrency(
                "SHOP",
                shopId.value(),
                "SELLER_PENDING",
                "VND"
        );
    }

    @Test
    void shouldUseSellerPendingAccountForUnsettledRefundAllocation() {
        PaymentAllocationId allocationId =
                new PaymentAllocationId(
                        UUID.randomUUID()
                );

        ShopId shopId = new ShopId(UUID.randomUUID());

        PaymentAllocationJpaEntity allocation =
                allocation(
                        allocationId,
                        shopId
                );

        UUID accountId =
                UUID.randomUUID();

        when(
                paymentAllocationRepository
                        .findById(
                                allocationId.value()
                        )
        ).thenReturn(
                Optional.of(allocation)
        );

        when(
                settlementLineRepository
                        .existsByPaymentAllocationId(
                                allocationId.value()
                        )
        ).thenReturn(false);

        whenSellerAccount(
                shopId,
                LedgerAccountType.SELLER_PENDING,
                accountId
        );

        Map<PaymentAllocationId, LedgerAccountId> result =
                adapter.sellerRefundAccountsFor(
                        List.of(allocationId)
                );

        assertEquals(
                new LedgerAccountId(accountId),
                result.get(allocationId)
        );

        verify(ledgerAccountRepository)
                .findByOwnerTypeAndOwnerIdAndAccountTypeAndCurrency(
                        "SHOP",
                        shopId.value(),
                        "SELLER_PENDING",
                        "VND"
                );
    }

    @Test
    void shouldUseSellerAvailableAccountForSettledRefundAllocation() {
        PaymentAllocationId allocationId = new PaymentAllocationId(UUID.randomUUID());

        ShopId shopId = new ShopId(UUID.randomUUID());

        when(
                paymentAllocationRepository
                        .findById(allocationId.value())
        ).thenReturn(
                Optional.of(
                        allocation(
                                allocationId,
                                shopId
                        )
                )
        );

        when(
                settlementLineRepository
                        .existsByPaymentAllocationId(
                                allocationId.value()
                        )
        ).thenReturn(true);

        UUID accountId =
                UUID.randomUUID();

        whenSellerAccount(
                shopId,
                LedgerAccountType.SELLER_AVAILABLE,
                accountId
        );

        Map<PaymentAllocationId, LedgerAccountId> result =
                adapter.sellerRefundAccountsFor(
                        List.of(allocationId)
                );

        assertEquals(
                new LedgerAccountId(accountId),
                result.get(allocationId)
        );

        verify(ledgerAccountRepository)
                .findByOwnerTypeAndOwnerIdAndAccountTypeAndCurrency(
                        "SHOP",
                        shopId.value(),
                        "SELLER_AVAILABLE",
                        "VND"
                );
    }

    @Test
    void shouldFindSellerAvailableAccount() {
        ShopId shopId =
                new ShopId(UUID.randomUUID());

        UUID accountId =
                UUID.randomUUID();

        whenSellerAccount(
                shopId,
                LedgerAccountType.SELLER_AVAILABLE,
                accountId
        );

        assertEquals(
                new LedgerAccountId(accountId),
                adapter.sellerAvailableAccount(
                        shopId
                )
        );
    }

    @Test
    void shouldRejectMissingSystemAccount() {
        when(
                ledgerAccountRepository
                        .findByAccountCodeAndCurrency(
                                "VNPAY_CLEARING:VND",
                                "VND"
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                LedgerAccountNotFoundException.class,
                () ->
                        adapter.vnpayClearingAccount()
        );
    }

    @Test
    void shouldRejectInactiveAccount() {
        LedgerAccountJpaEntity entity =
                systemAccount(
                        UUID.randomUUID(),
                        LedgerAccountType.VNPAY_CLEARING
                );

        entity.setStatus("CLOSED");

        when(
                ledgerAccountRepository
                        .findByAccountCodeAndCurrency(
                                "VNPAY_CLEARING:VND",
                                "VND"
                        )
        ).thenReturn(
                Optional.of(entity)
        );

        assertThrows(
                LedgerAccountNotFoundException.class,
                () ->
                        adapter.vnpayClearingAccount()
        );
    }

    @Test
    void shouldRejectMissingPaymentAllocationForRefund() {
        PaymentAllocationId allocationId =
                new PaymentAllocationId(
                        UUID.randomUUID()
                );

        when(
                paymentAllocationRepository
                        .findById(
                                allocationId.value()
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                PaymentAllocationNotFoundException.class,
                () ->
                        adapter.sellerRefundAccountsFor(
                                List.of(allocationId)
                        )
        );

        verifyNoInteractions(
                settlementLineRepository
        );
    }

    private void assertSystemAccount(
            LedgerAccountType type,
            AccountSupplier supplier
    ) {
        UUID id = UUID.randomUUID();

        when(
                ledgerAccountRepository
                        .findByAccountCodeAndCurrency(
                                type.name() + ":VND",
                                "VND"
                        )
        ).thenReturn(
                Optional.of(
                        systemAccount(
                                id,
                                type
                        )
                )
        );

        assertEquals(
                new LedgerAccountId(id),
                supplier.get()
        );
    }

    private void whenSellerAccount(
            ShopId shopId,
            LedgerAccountType type,
            UUID accountId
    ) {
        LedgerAccountJpaEntity entity =
                new LedgerAccountJpaEntity();

        entity.setId(accountId);

        entity.setAccountCode(
                type.name()
                        + ":"
                        + shopId.value()
                        + ":VND"
        );

        entity.setAccountType(type.name());

        entity.setOwnerType("SHOP");
        entity.setOwnerId(shopId.value());
        entity.setCurrency("VND");
        entity.setStatus("ACTIVE");

        when(
                ledgerAccountRepository
                        .findByOwnerTypeAndOwnerIdAndAccountTypeAndCurrency(
                                "SHOP",
                                shopId.value(),
                                type.name(),
                                "VND"
                        )
        ).thenReturn(
                Optional.of(entity)
        );
    }

    private LedgerAccountJpaEntity systemAccount(
            UUID id,
            LedgerAccountType type
    ) {
        LedgerAccountJpaEntity entity = new LedgerAccountJpaEntity();

        entity.setId(id);

        entity.setAccountCode(type.name() + ":VND");

        entity.setAccountType(type.name());

        entity.setOwnerType("SYSTEM");
        entity.setOwnerId(null);
        entity.setCurrency("VND");
        entity.setStatus("ACTIVE");

        return entity;
    }

    private PaymentAllocationJpaEntity allocation(
            PaymentAllocationId allocationId,
            ShopId shopId
    ) {
        PaymentAllocationJpaEntity entity =
                new PaymentAllocationJpaEntity();

        entity.setId(allocationId.value());

        entity.setShopId(shopId.value());

        return entity;
    }

    @FunctionalInterface
    private interface AccountSupplier {

        LedgerAccountId get();
    }
}