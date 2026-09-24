package com.taca.paymentwallet.infrastructure.persistence.mapper;

import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.payment.PaymentMethod;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.payment.PaymentStatus;
import com.taca.paymentwallet.domain.payout.BankAccountSnapshot;
import com.taca.paymentwallet.domain.payout.Payout;
import com.taca.paymentwallet.domain.payout.PayoutStatus;
import com.taca.paymentwallet.domain.refund.Refund;
import com.taca.paymentwallet.domain.refund.RefundAllocation;
import com.taca.paymentwallet.domain.refund.RefundStatus;
import com.taca.paymentwallet.domain.settlement.SettlementBatch;
import com.taca.paymentwallet.domain.settlement.SettlementBatchItem;
import com.taca.paymentwallet.domain.settlement.SettlementBatchItemStatus;
import com.taca.paymentwallet.domain.settlement.SettlementBatchStatus;
import com.taca.paymentwallet.domain.settlement.SettlementLine;
import com.taca.paymentwallet.domain.valueobject.*;
import com.taca.paymentwallet.domain.wallet.*;
import com.taca.paymentwallet.infrastructure.persistence.entity.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceMapperRoundTripTest {

    private static final LocalDateTime CREATED_AT =
            LocalDateTime.of(
                    2026,
                    9,
                    24,
                    1,
                    0
            );

    private static final LocalDateTime UPDATED_AT =
            LocalDateTime.of(
                    2026,
                    9,
                    24,
                    2,
                    0
            );

    @Test
    void shouldRoundTripUtcTimestamp() {
        Instant original =
                Instant.parse(
                        "2026-09-24T01:23:45.123456Z"
                );

        LocalDateTime persisted =
                PersistenceTimeMapper
                        .toLocalDateTime(original);

        Instant restored =
                PersistenceTimeMapper
                        .toInstant(persisted);

        assertEquals(
                original,
                restored
        );
    }

    @Test
    void shouldHandleNullTimestamp() {
        assertNull(
                PersistenceTimeMapper
                        .toLocalDateTime(null)
        );

        assertNull(
                PersistenceTimeMapper
                        .toInstant(null)
        );
    }

    @Test
    void shouldRoundTripBankAccountSnapshot() {
        BankAccountSnapshotPersistenceCodec codec =
                new BankAccountSnapshotPersistenceCodec();

        BankAccountSnapshot original =
                new BankAccountSnapshot(
                        "VCB",
                        "NGUYEN VAN A",
                        "******1234"
                );

        String encoded =
                codec.encode(original);

        BankAccountSnapshot restored =
                codec.decode(encoded);

        assertEquals(
                original,
                restored
        );

        assertTrue(
                encoded.startsWith("v1.")
        );
    }

    @Test
    void shouldRoundTripPaymentAndOrders() {
        PaymentPersistenceMapper mapper =
                new PaymentPersistenceMapper();

        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());

        CheckoutGroupId checkoutGroupId =
                new CheckoutGroupId(UUID.randomUUID());

        BuyerUserId buyerUserId =
                new BuyerUserId(UUID.randomUUID());

        OrderId orderId1 =
                new OrderId(UUID.randomUUID());

        OrderId orderId2 =
                new OrderId(UUID.randomUUID());

        ShopId shopId1 =
                new ShopId(UUID.randomUUID());

        ShopId shopId2 =
                new ShopId(UUID.randomUUID());

        Instant expiresAt =
                Instant.parse(
                        "2026-09-24T02:15:00Z"
                );

        Payment original =
                Payment.create(
                        paymentId,
                        checkoutGroupId,
                        buyerUserId,
                        PaymentMethod.VNPAY,
                        Money.vnd(150_000),
                        List.of(
                                new PaymentOrder(
                                        orderId1,
                                        shopId1,
                                        Money.vnd(100_000)
                                ),
                                new PaymentOrder(
                                        orderId2,
                                        shopId2,
                                        Money.vnd(50_000)
                                )
                        ),
                        expiresAt
                );

        PaymentJpaEntity paymentEntity =
                mapper.toNewEntity(
                        original,
                        CREATED_AT
                );

        PaymentOrderJpaEntity orderEntity1 =
                mapper.toOrderEntity(
                        paymentId,
                        original.orders().get(0),
                        UUID.randomUUID(),
                        CREATED_AT
                );

        PaymentOrderJpaEntity orderEntity2 =
                mapper.toOrderEntity(
                        paymentId,
                        original.orders().get(1),
                        UUID.randomUUID(),
                        CREATED_AT
                );

        Payment restored =
                mapper.toDomain(
                        paymentEntity,
                        List.of(
                                orderEntity1,
                                orderEntity2
                        )
                );

        assertEquals(
                original.id(),
                restored.id()
        );

        assertEquals(
                original.checkoutGroupId(),
                restored.checkoutGroupId()
        );

        assertEquals(
                original.buyerUserId(),
                restored.buyerUserId()
        );

        assertEquals(
                original.method(),
                restored.method()
        );

        assertEquals(
                original.amount(),
                restored.amount()
        );

        assertEquals(
                original.status(),
                restored.status()
        );

        assertEquals(
                original.capturedAmount(),
                restored.capturedAmount()
        );

        assertEquals(
                original.refundedAmount(),
                restored.refundedAmount()
        );

        assertEquals(
                original.expiresAt(),
                restored.expiresAt()
        );

        assertEquals(
                original.paidAt(),
                restored.paidAt()
        );

        assertEquals(
                original.orders(),
                restored.orders()
        );

        assertTrue(
                restored.domainEvents().isEmpty()
        );
    }

    @Test
    void shouldUpdatePaymentEntityWithoutReplacingPersistenceMetadata() {
        PaymentPersistenceMapper mapper =
                new PaymentPersistenceMapper();

        Payment payment =
                Payment.create(
                        new PaymentId(UUID.randomUUID()),
                        new CheckoutGroupId(UUID.randomUUID()),
                        new BuyerUserId(UUID.randomUUID()),
                        PaymentMethod.COD,
                        Money.vnd(100_000),
                        List.of(
                                new PaymentOrder(
                                        new OrderId(UUID.randomUUID()),
                                        new ShopId(UUID.randomUUID()),
                                        Money.vnd(100_000)
                                )
                        )
                );

        PaymentJpaEntity entity =
                mapper.toNewEntity(
                        payment,
                        CREATED_AT
                );

        entity.setVersion(7L);

        payment.markSucceeded(
                Instant.parse(
                        "2026-09-24T02:00:00Z"
                )
        );

        mapper.updateEntity(
                payment,
                entity,
                UPDATED_AT
        );

        assertEquals(
                7L,
                entity.getVersion()
        );

        assertEquals(
                CREATED_AT,
                entity.getCreatedAt()
        );

        assertEquals(
                UPDATED_AT,
                entity.getUpdatedAt()
        );

        assertEquals(
                "SUCCESS",
                entity.getStatus()
        );

        assertEquals(
                100_000L,
                entity.getCapturedAmount()
        );
    }

    @Test
    void shouldRoundTripPaymentAllocation() {
        PaymentAllocationPersistenceMapper mapper =
                new PaymentAllocationPersistenceMapper();

        PaymentAllocation original =
                new PaymentAllocation(
                        new PaymentAllocationId(
                                UUID.randomUUID()
                        ),
                        new PaymentId(
                                UUID.randomUUID()
                        ),
                        new OrderId(
                                UUID.randomUUID()
                        ),
                        new ShopId(
                                UUID.randomUUID()
                        ),
                        new WalletId(
                                UUID.randomUUID()
                        ),
                        Money.vnd(100_000),
                        Money.vnd(7_000),
                        Money.vnd(1_000),
                        Money.vnd(92_000),
                        new FeeConfigId(
                                UUID.randomUUID()
                        ),
                        new TaxConfigId(
                                UUID.randomUUID()
                        )
                );

        PaymentAllocationJpaEntity entity =
                mapper.toEntity(
                        original,
                        CREATED_AT
                );

        PaymentAllocation restored =
                mapper.toDomain(entity);

        assertEquals(
                original,
                restored
        );

        assertEquals(
                CREATED_AT,
                entity.getCreatedAt()
        );
    }

    @Test
    void shouldRoundTripWallet() {
        WalletPersistenceMapper mapper =
                new WalletPersistenceMapper();

        Wallet original =
                Wallet.create(
                        new WalletId(UUID.randomUUID()),
                        new ShopId(UUID.randomUUID())
                );

        original.creditPending(
                Money.vnd(92_000)
        );

        WalletJpaEntity entity =
                mapper.toNewEntity(
                        original,
                        CREATED_AT
                );

        Wallet restored =
                mapper.toDomain(entity);

        assertEquals(
                original.id(),
                restored.id()
        );

        assertEquals(
                original.shopId(),
                restored.shopId()
        );

        assertEquals(
                original.currency(),
                restored.currency()
        );

        assertEquals(
                original.availableBalance(),
                restored.availableBalance()
        );

        assertEquals(
                original.pendingBalance(),
                restored.pendingBalance()
        );

        assertEquals(
                original.status(),
                restored.status()
        );
    }

    @Test
    void shouldUpdateWalletWithoutReplacingVersionOrCreatedAt() {
        WalletPersistenceMapper mapper =
                new WalletPersistenceMapper();

        Wallet wallet =
                Wallet.create(
                        new WalletId(UUID.randomUUID()),
                        new ShopId(UUID.randomUUID())
                );

        WalletJpaEntity entity =
                mapper.toNewEntity(
                        wallet,
                        CREATED_AT
                );

        entity.setVersion(4L);

        wallet.creditPending(
                Money.vnd(50_000)
        );

        mapper.updateEntity(
                wallet,
                entity,
                UPDATED_AT
        );

        assertEquals(
                4L,
                entity.getVersion()
        );

        assertEquals(
                CREATED_AT,
                entity.getCreatedAt()
        );

        assertEquals(
                UPDATED_AT,
                entity.getUpdatedAt()
        );

        assertEquals(
                50_000L,
                entity.getPendingBalance()
        );
    }

    @Test
    void shouldRoundTripRefund() {
        RefundPersistenceMapper mapper =
                new RefundPersistenceMapper();

        Refund original =
                Refund.rehydrate(
                        new RefundId(UUID.randomUUID()),
                        new PaymentId(UUID.randomUUID()),
                        Money.vnd(50_000),
                        "Buyer requested refund",
                        new IdempotencyKey(
                                "refund-idem-001"
                        ),
                        RefundStatus.FAILED,
                        "VNPAY_REFUND_FAILED"
                );

        RefundJpaEntity entity =
                mapper.toNewEntity(
                        original,
                        CREATED_AT
                );

        Refund restored =
                mapper.toDomain(entity);

        assertEquals(
                original.id(),
                restored.id()
        );

        assertEquals(
                original.paymentId(),
                restored.paymentId()
        );

        assertEquals(
                original.amount(),
                restored.amount()
        );

        assertEquals(
                original.reason(),
                restored.reason()
        );

        assertEquals(
                original.idempotencyKey(),
                restored.idempotencyKey()
        );

        assertEquals(
                original.status(),
                restored.status()
        );

        assertEquals(
                original.failureCode(),
                restored.failureCode()
        );

        assertTrue(
                restored.domainEvents().isEmpty()
        );
    }

    @Test
    void shouldRoundTripRefundAllocation() {
        RefundAllocationPersistenceMapper mapper =
                new RefundAllocationPersistenceMapper();

        RefundId refundId =
                new RefundId(UUID.randomUUID());

        UUID rowId =
                UUID.randomUUID();

        RefundAllocation original =
                new RefundAllocation(
                        new PaymentAllocationId(
                                UUID.randomUUID()
                        ),
                        Money.vnd(50_000),
                        Money.vnd(3_500),
                        Money.vnd(500),
                        Money.vnd(46_000)
                );

        RefundAllocationJpaEntity entity =
                mapper.toEntity(
                        refundId,
                        original,
                        rowId,
                        CREATED_AT
                );

        RefundAllocation restored =
                mapper.toDomain(entity);

        assertEquals(
                original,
                restored
        );

        assertEquals(
                rowId,
                entity.getId()
        );

        assertEquals(
                refundId.value(),
                entity.getRefundId()
        );
    }

    @Test
    void shouldRoundTripPayout() {
        BankAccountSnapshotPersistenceCodec codec =
                new BankAccountSnapshotPersistenceCodec();

        PayoutPersistenceMapper mapper =
                new PayoutPersistenceMapper(codec);

        Payout original =
                Payout.rehydrate(
                        new PayoutId(UUID.randomUUID()),
                        new WalletId(UUID.randomUUID()),
                        new ShopId(UUID.randomUUID()),
                        Money.vnd(200_000),
                        new BankAccountSnapshot(
                                "VCB",
                                "NGUYEN VAN A",
                                "******5678"
                        ),
                        new IdempotencyKey(
                                "payout-idem-001"
                        ),
                        PayoutStatus.SUCCESS,
                        "BANK-REF-001",
                        null
                );

        PayoutJpaEntity entity =
                mapper.toNewEntity(
                        original,
                        CREATED_AT
                );

        Payout restored =
                mapper.toDomain(entity);

        assertEquals(
                original.id(),
                restored.id()
        );

        assertEquals(
                original.walletId(),
                restored.walletId()
        );

        assertEquals(
                original.shopId(),
                restored.shopId()
        );

        assertEquals(
                original.amount(),
                restored.amount()
        );

        assertEquals(
                original.bankAccountSnapshot(),
                restored.bankAccountSnapshot()
        );

        assertEquals(
                original.idempotencyKey(),
                restored.idempotencyKey()
        );

        assertEquals(
                original.status(),
                restored.status()
        );

        assertEquals(
                original.providerRef(),
                restored.providerRef()
        );

        assertEquals(
                original.failureCode(),
                restored.failureCode()
        );

        assertTrue(
                restored.domainEvents().isEmpty()
        );
    }

    @Test
    void shouldRoundTripLedgerPostingAndEntries() {
        LedgerPostingPersistenceMapper mapper =
                new LedgerPostingPersistenceMapper();

        LedgerPostingId postingId =
                new LedgerPostingId(UUID.randomUUID());

        UUID referenceId =
                UUID.randomUUID();

        LedgerAccountId debitAccount =
                new LedgerAccountId(UUID.randomUUID());

        LedgerAccountId creditAccount =
                new LedgerAccountId(UUID.randomUUID());

        LedgerPosting original =
                new LedgerPosting(
                        postingId,
                        "PAYMENT_CAPTURE",
                        "PAYMENT_CAPTURE:" + referenceId,
                        "PAYMENT",
                        referenceId.toString(),
                        List.of(
                                LedgerEntry.debit(
                                        debitAccount,
                                        Money.vnd(100_000)
                                ),
                                LedgerEntry.credit(
                                        creditAccount,
                                        Money.vnd(100_000)
                                )
                        )
                );

        LedgerPostingJpaEntity postingEntity =
                mapper.toPostingEntity(
                        original,
                        CREATED_AT
                );

        LedgerEntryJpaEntity firstEntry =
                mapper.toEntryEntity(
                        postingId,
                        original.entries().get(0),
                        UUID.randomUUID(),
                        CREATED_AT
                );

        LedgerEntryJpaEntity secondEntry =
                mapper.toEntryEntity(
                        postingId,
                        original.entries().get(1),
                        UUID.randomUUID(),
                        CREATED_AT
                );

        LedgerPosting restored =
                mapper.toDomain(
                        postingEntity,
                        List.of(
                                firstEntry,
                                secondEntry
                        )
                );

        assertEquals(
                original.id(),
                restored.id()
        );

        assertEquals(
                original.postingType(),
                restored.postingType()
        );

        assertEquals(
                original.businessKey(),
                restored.businessKey()
        );

        assertEquals(
                original.referenceType(),
                restored.referenceType()
        );

        assertEquals(
                original.referenceId(),
                restored.referenceId()
        );

        assertEquals(
                original.entries(),
                restored.entries()
        );
    }

    @Test
    void shouldMapLedgerAccountToDomain() {
        LedgerAccountPersistenceMapper mapper =
                new LedgerAccountPersistenceMapper();

        UUID accountId =
                UUID.randomUUID();

        UUID shopId =
                UUID.randomUUID();

        LedgerAccountJpaEntity entity =
                new LedgerAccountJpaEntity();

        entity.setId(accountId);
        entity.setAccountCode(
                "SELLER_PENDING:" + shopId
        );
        entity.setAccountType(
                "SELLER_PENDING"
        );
        entity.setOwnerType(
                "SHOP"
        );
        entity.setOwnerId(shopId);
        entity.setCurrency("VND");
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(CREATED_AT);

        LedgerAccount account =
                mapper.toDomain(entity);

        assertEquals(
                new LedgerAccountId(accountId),
                account.id()
        );

        assertEquals(
                LedgerAccountType.SELLER_PENDING,
                account.accountType()
        );

        assertEquals(
                "SHOP",
                account.ownerType()
        );

        assertEquals(
                new ShopId(shopId),
                account.ownerId()
        );

        assertEquals(
                "VND",
                account.currency()
        );
    }

    @Test
    void shouldRoundTripCompletedSettlementBatch() {
        SettlementPersistenceMapper mapper =
                new SettlementPersistenceMapper();

        SettlementBatchId batchId =
                new SettlementBatchId(
                        UUID.randomUUID()
                );

        SettlementBatchItemId itemId =
                new SettlementBatchItemId(
                        UUID.randomUUID()
                );

        SettlementLineId lineId =
                new SettlementLineId(
                        UUID.randomUUID()
                );

        PaymentAllocationId allocationId =
                new PaymentAllocationId(
                        UUID.randomUUID()
                );

        ShopId shopId =
                new ShopId(
                        UUID.randomUUID()
                );

        WalletId walletId =
                new WalletId(
                        UUID.randomUUID()
                );

        LedgerPostingId postingId =
                new LedgerPostingId(
                        UUID.randomUUID()
                );

        SettlementLine line =
                new SettlementLine(
                        lineId,
                        allocationId,
                        Money.vnd(92_000)
                );

        SettlementBatchItem item =
                SettlementBatchItem.rehydrate(
                        itemId,
                        shopId,
                        walletId,
                        Money.vnd(100_000),
                        Money.vnd(7_000),
                        Money.vnd(1_000),
                        Money.vnd(92_000),
                        Money.vnd(92_000),
                        Money.vnd(0),
                        List.of(line),
                        SettlementBatchItemStatus.COMPLETED,
                        postingId
                );

        Instant periodStart =
                Instant.parse(
                        "2026-09-01T00:00:00Z"
                );

        Instant periodEnd =
                Instant.parse(
                        "2026-09-02T00:00:00Z"
                );

        SettlementBatch original =
                SettlementBatch.rehydrate(
                        batchId,
                        periodStart,
                        periodEnd,
                        List.of(item),
                        SettlementBatchStatus.COMPLETED
                );

        SettlementBatchJpaEntity batchEntity =
                mapper.toBatchEntity(
                        original,
                        CREATED_AT,
                        UPDATED_AT,
                        null
                );

        SettlementBatchItemJpaEntity itemEntity =
                mapper.toItemEntity(
                        batchId,
                        item,
                        CREATED_AT
                );

        SettlementLineJpaEntity lineEntity =
                mapper.toLineEntity(
                        itemId,
                        line,
                        CREATED_AT
                );

        SettlementBatch restored =
                mapper.toDomain(
                        batchEntity,
                        List.of(itemEntity),
                        Map.of(
                                itemId.value(),
                                List.of(lineEntity)
                        )
                );

        assertEquals(
                original.id(),
                restored.id()
        );

        assertEquals(
                original.periodStart(),
                restored.periodStart()
        );

        assertEquals(
                original.periodEnd(),
                restored.periodEnd()
        );

        assertEquals(
                SettlementBatchStatus.COMPLETED,
                restored.status()
        );

        assertEquals(
                1,
                restored.items().size()
        );

        SettlementBatchItem restoredItem =
                restored.items().getFirst();

        assertEquals(
                itemId,
                restoredItem.id()
        );

        assertEquals(
                shopId,
                restoredItem.shopId()
        );

        assertEquals(
                walletId,
                restoredItem.walletId()
        );

        assertEquals(
                SettlementBatchItemStatus.COMPLETED,
                restoredItem.status()
        );

        assertEquals(
                postingId,
                restoredItem.postingId()
        );

        assertEquals(
                List.of(line),
                restoredItem.lines()
        );

        assertEquals(
                Money.vnd(92_000),
                restored.totalReleased()
        );

        assertTrue(
                restored.domainEvents().isEmpty()
        );

        assertEquals(
                1,
                batchEntity.getShopCount()
        );

        assertEquals(
                100_000L,
                batchEntity.getTotalGross()
        );

        assertEquals(
                7_000L,
                batchEntity.getTotalCommission()
        );

        assertEquals(
                1_000L,
                batchEntity.getTotalTax()
        );

        assertEquals(
                92_000L,
                batchEntity.getTotalNet()
        );

        assertEquals(
                92_000L,
                batchEntity.getTotalReleased()
        );

        assertEquals(
                0L,
                batchEntity.getTotalHeld()
        );
    }

    @Test
    void shouldRejectUpdatingPaymentEntityWithDifferentId() {
        PaymentPersistenceMapper mapper =
                new PaymentPersistenceMapper();

        Payment payment =
                Payment.create(
                        new PaymentId(UUID.randomUUID()),
                        new CheckoutGroupId(UUID.randomUUID()),
                        new BuyerUserId(UUID.randomUUID()),
                        PaymentMethod.COD,
                        Money.vnd(100_000),
                        List.of(
                                new PaymentOrder(
                                        new OrderId(UUID.randomUUID()),
                                        new ShopId(UUID.randomUUID()),
                                        Money.vnd(100_000)
                                )
                        )
                );

        PaymentJpaEntity entity =
                mapper.toNewEntity(
                        payment,
                        CREATED_AT
                );

        entity.setId(
                UUID.randomUUID()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.updateEntity(
                        payment,
                        entity,
                        UPDATED_AT
                )
        );
    }

    @Test
    void shouldRejectUpdatingWalletEntityWithDifferentId() {
        WalletPersistenceMapper mapper =
                new WalletPersistenceMapper();

        Wallet wallet =
                Wallet.create(
                        new WalletId(UUID.randomUUID()),
                        new ShopId(UUID.randomUUID())
                );

        WalletJpaEntity entity =
                mapper.toNewEntity(
                        wallet,
                        CREATED_AT
                );

        entity.setId(
                UUID.randomUUID()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.updateEntity(
                        wallet,
                        entity,
                        UPDATED_AT
                )
        );
    }
}