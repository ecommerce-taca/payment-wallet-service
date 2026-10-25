package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.RetryAdminSettlementCommand;
import com.taca.paymentwallet.application.exception.SettlementBatchNotFoundException;
import com.taca.paymentwallet.application.port.out.*;
import com.taca.paymentwallet.application.result.RetryAdminSettlementResult;
import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.settlement.*;
import com.taca.paymentwallet.domain.valueobject.*;
import com.taca.paymentwallet.domain.wallet.LedgerPosting;
import com.taca.paymentwallet.domain.wallet.LedgerPostingFactory;
import com.taca.paymentwallet.domain.wallet.Wallet;
import com.taca.paymentwallet.domain.wallet.WalletStatus;
import com.taca.paymentwallet.support.FakeBusinessMetricsPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RetryAdminSettlementServiceTest {

    private SettlementRepositoryPort settlementRepository;
    private WalletRepositoryPort walletRepository;
    private LedgerPostingRepositoryPort ledgerPostingRepository;
    private LedgerAccountLookupPort ledgerAccountLookupPort;
    private OutboxPort outboxPort;
    private AdminAuditPort auditPort;
    private IdGeneratorPort idGeneratorPort;
    private ClockPort clockPort;
    private RetryAdminSettlementService service;

    @BeforeEach
    void setUp() {
        settlementRepository = mock(SettlementRepositoryPort.class);
        walletRepository = mock(WalletRepositoryPort.class);
        ledgerPostingRepository = mock(LedgerPostingRepositoryPort.class);
        ledgerAccountLookupPort = mock(LedgerAccountLookupPort.class);
        outboxPort = mock(OutboxPort.class);
        auditPort = mock(AdminAuditPort.class);
        idGeneratorPort = mock(IdGeneratorPort.class);
        clockPort = mock(ClockPort.class);

        service = new RetryAdminSettlementService(
                settlementRepository,
                walletRepository,
                ledgerPostingRepository,
                ledgerAccountLookupPort,
                outboxPort,
                auditPort,
                new DirectTransactionPort(),
                idGeneratorPort,
                new LedgerPostingFactory(),
                clockPort,
                new FakeBusinessMetricsPort()
        );
    }

    @Test
    void shouldRetryFailedSettlementBatch() {
        UUID actorUserId = UUID.randomUUID();
        SettlementBatchItem failedItem = failedItem();
        SettlementBatch batch = failedBatch(List.of(failedItem));
        Wallet wallet = wallet(failedItem);
        LedgerAccountId pendingAccount = new LedgerAccountId(UUID.randomUUID());
        LedgerAccountId availableAccount = new LedgerAccountId(UUID.randomUUID());
        LedgerPostingId postingId = new LedgerPostingId(UUID.randomUUID());
        Instant now = Instant.parse("2026-10-23T15:00:00Z");

        when(settlementRepository.findByIdForUpdate(batch.id()))
                .thenReturn(Optional.of(batch));
        when(walletRepository.findByIdForUpdate(failedItem.walletId()))
                .thenReturn(Optional.of(wallet));
        when(ledgerAccountLookupPort.sellerPendingAccountsFor(List.of(failedItem.shopId())))
                .thenReturn(Map.of(failedItem.shopId(), pendingAccount));
        when(ledgerAccountLookupPort.sellerAvailableAccount(failedItem.shopId()))
                .thenReturn(availableAccount);
        when(idGeneratorPort.nextLedgerPostingId()).thenReturn(postingId);
        when(clockPort.now()).thenReturn(now);
        when(settlementRepository.update(batch)).thenReturn(batch);

        RetryAdminSettlementResult result = service.execute(
                new RetryAdminSettlementCommand(batch.id().value(), actorUserId)
        );

        assertThat(result.batchId()).isEqualTo(batch.id().value());
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.retriedItemCount()).isEqualTo(1);
        assertThat(batch.status()).isEqualTo(SettlementBatchStatus.COMPLETED);
        assertThat(failedItem.status()).isEqualTo(SettlementBatchItemStatus.COMPLETED);
        assertThat(failedItem.postingId()).isEqualTo(postingId);
        assertThat(wallet.pendingBalance()).isEqualTo(Money.vnd(8_000));
        assertThat(wallet.availableBalance()).isEqualTo(Money.vnd(92_000));

        verify(walletRepository).save(wallet);
        verify(ledgerPostingRepository).save(any(LedgerPosting.class));
        verify(settlementRepository).update(batch);
    }

    @Test
    void shouldSkipAlreadyCompletedItems() {
        UUID actorUserId = UUID.randomUUID();

        SettlementBatchItem completedItem = completedItem();
        SettlementBatchItem failedItem = failedItem();
        SettlementBatch batch = failedBatch(List.of(completedItem, failedItem));

        Wallet failedWallet = wallet(failedItem);
        LedgerAccountId pendingAccount = new LedgerAccountId(UUID.randomUUID());
        LedgerAccountId availableAccount = new LedgerAccountId(UUID.randomUUID());

        when(settlementRepository.findByIdForUpdate(batch.id()))
                .thenReturn(Optional.of(batch));
        when(walletRepository.findByIdForUpdate(failedItem.walletId()))
                .thenReturn(Optional.of(failedWallet));
        when(ledgerAccountLookupPort.sellerPendingAccountsFor(List.of(failedItem.shopId())))
                .thenReturn(Map.of(failedItem.shopId(), pendingAccount));
        when(ledgerAccountLookupPort.sellerAvailableAccount(failedItem.shopId()))
                .thenReturn(availableAccount);
        when(idGeneratorPort.nextLedgerPostingId())
                .thenReturn(new LedgerPostingId(UUID.randomUUID()));
        when(clockPort.now()).thenReturn(Instant.parse("2026-10-23T15:00:00Z"));
        when(settlementRepository.update(batch)).thenReturn(batch);

        RetryAdminSettlementResult result = service.execute(
                new RetryAdminSettlementCommand(batch.id().value(), actorUserId)
        );

        assertThat(result.retriedItemCount()).isEqualTo(1);
        assertThat(completedItem.status()).isEqualTo(SettlementBatchItemStatus.COMPLETED);
        assertThat(completedItem.postingId()).isNotNull();
        assertThat(failedItem.status()).isEqualTo(SettlementBatchItemStatus.COMPLETED);

        verify(walletRepository, never()).findByIdForUpdate(completedItem.walletId());
        verify(walletRepository).findByIdForUpdate(failedItem.walletId());
        verify(ledgerPostingRepository, times(1)).save(any(LedgerPosting.class));
    }

    @Test
    void shouldReturnNotFoundWhenBatchDoesNotExist() {
        UUID batchId = UUID.randomUUID();

        when(settlementRepository.findByIdForUpdate(new SettlementBatchId(batchId)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(
                new RetryAdminSettlementCommand(batchId, UUID.randomUUID())
        ))
                .isInstanceOf(SettlementBatchNotFoundException.class)
                .hasMessageContaining(batchId.toString());

        verifyNoInteractions(walletRepository);
        verifyNoInteractions(ledgerPostingRepository);
        verifyNoInteractions(auditPort);
        verifyNoInteractions(outboxPort);
    }

    @Test
    void shouldRejectNonFailedBatch() {
        SettlementBatch batch = pendingBatch();

        when(settlementRepository.findByIdForUpdate(batch.id()))
                .thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> service.execute(
                new RetryAdminSettlementCommand(batch.id().value(), UUID.randomUUID())
        ))
                .isInstanceOf(InvalidSettlementStateException.class)
                .hasMessage("Only failed settlement batch can be retried");

        verify(settlementRepository, never()).update(any());
        verifyNoInteractions(walletRepository);
        verifyNoInteractions(ledgerPostingRepository);
        verifyNoInteractions(auditPort);
    }

    @Test
    void shouldWriteAdminAudit() {
        UUID actorUserId = UUID.randomUUID();
        SettlementBatchItem item = failedItem();
        SettlementBatch batch = failedBatch(List.of(item));
        Instant now = Instant.parse("2026-10-23T15:00:00Z");

        prepareSuccessfulRetry(batch, item, now);

        service.execute(
                new RetryAdminSettlementCommand(batch.id().value(), actorUserId)
        );

        verify(auditPort).append(
                actorUserId,
                "RETRY_SETTLEMENT_BATCH",
                "SETTLEMENT_BATCH",
                batch.id().value(),
                null,
                now
        );
    }

    @Test
    void shouldPublishCompletedEvent() {
        SettlementBatchItem item = failedItem();
        SettlementBatch batch = failedBatch(List.of(item));

        prepareSuccessfulRetry(
                batch,
                item,
                Instant.parse("2026-10-23T15:00:00Z")
        );

        service.execute(
                new RetryAdminSettlementCommand(
                        batch.id().value(),
                        UUID.randomUUID()
                )
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(outboxPort).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(1);
        assertThat(batch.domainEvents()).isEmpty();
    }

    private void prepareSuccessfulRetry(
            SettlementBatch batch,
            SettlementBatchItem item,
            Instant now
    ) {
        Wallet wallet = wallet(item);
        LedgerAccountId pendingAccount = new LedgerAccountId(UUID.randomUUID());
        LedgerAccountId availableAccount = new LedgerAccountId(UUID.randomUUID());

        when(settlementRepository.findByIdForUpdate(batch.id()))
                .thenReturn(Optional.of(batch));
        when(walletRepository.findByIdForUpdate(item.walletId()))
                .thenReturn(Optional.of(wallet));
        when(ledgerAccountLookupPort.sellerPendingAccountsFor(List.of(item.shopId())))
                .thenReturn(Map.of(item.shopId(), pendingAccount));
        when(ledgerAccountLookupPort.sellerAvailableAccount(item.shopId()))
                .thenReturn(availableAccount);
        when(idGeneratorPort.nextLedgerPostingId())
                .thenReturn(new LedgerPostingId(UUID.randomUUID()));
        when(clockPort.now()).thenReturn(now);
        when(settlementRepository.update(batch)).thenReturn(batch);
    }

    private SettlementBatch pendingBatch() {
        SettlementBatchItem item = pendingItem();

        return SettlementBatch.create(
                new SettlementBatchId(UUID.randomUUID()),
                Instant.parse("2026-10-01T00:00:00Z"),
                Instant.parse("2026-10-08T00:00:00Z"),
                List.of(item)
        );
    }

    private SettlementBatch failedBatch(List<SettlementBatchItem> items) {
        return SettlementBatch.rehydrate(
                new SettlementBatchId(UUID.randomUUID()),
                Instant.parse("2026-10-01T00:00:00Z"),
                Instant.parse("2026-10-08T00:00:00Z"),
                items,
                SettlementBatchStatus.FAILED
        );
    }

    private SettlementBatchItem failedItem() {
        SettlementBatchItem item = pendingItem();
        item.markFailed();
        return item;
    }

    private SettlementBatchItem completedItem() {
        SettlementBatchItem item = pendingItem();
        item.markCompleted(new LedgerPostingId(UUID.randomUUID()));
        return item;
    }

    private SettlementBatchItem pendingItem() {
        SettlementLine line = new SettlementLine(
                new SettlementLineId(UUID.randomUUID()),
                new PaymentAllocationId(UUID.randomUUID()),
                Money.vnd(92_000)
        );

        return SettlementBatchItem.create(
                new SettlementBatchItemId(UUID.randomUUID()),
                new ShopId(UUID.randomUUID()),
                new WalletId(UUID.randomUUID()),
                Money.vnd(100_000),
                Money.vnd(7_000),
                Money.vnd(1_000),
                Money.vnd(92_000),
                Money.vnd(92_000),
                Money.vnd(0),
                List.of(line)
        );
    }

    private Wallet wallet(SettlementBatchItem item) {
        return Wallet.rehydrate(
                item.walletId(),
                item.shopId(),
                "VND",
                Money.vnd(0),
                Money.vnd(100_000),
                WalletStatus.ACTIVE
        );
    }

    private static class DirectTransactionPort implements TransactionPort {

        @Override
        public <T> T execute(Supplier<T> action) {
            return action.get();
        }
    }
}