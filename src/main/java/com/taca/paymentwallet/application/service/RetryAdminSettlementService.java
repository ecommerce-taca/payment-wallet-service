package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.RetryAdminSettlementCommand;
import com.taca.paymentwallet.application.exception.SettlementBatchNotFoundException;
import com.taca.paymentwallet.application.exception.WalletNotFoundException;
import com.taca.paymentwallet.application.port.in.RetryAdminSettlementUseCase;
import com.taca.paymentwallet.application.port.out.*;
import com.taca.paymentwallet.application.result.RetryAdminSettlementResult;
import com.taca.paymentwallet.domain.settlement.SettlementBatch;
import com.taca.paymentwallet.domain.settlement.SettlementBatchItem;
import com.taca.paymentwallet.domain.settlement.SettlementBatchItemStatus;
import com.taca.paymentwallet.domain.valueobject.LedgerAccountId;
import com.taca.paymentwallet.domain.valueobject.SettlementBatchId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.wallet.LedgerPosting;
import com.taca.paymentwallet.domain.wallet.LedgerPostingFactory;
import com.taca.paymentwallet.domain.wallet.Wallet;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RetryAdminSettlementService implements RetryAdminSettlementUseCase {

    private final SettlementRepositoryPort settlementRepository;
    private final WalletRepositoryPort walletRepository;
    private final LedgerPostingRepositoryPort ledgerPostingRepository;
    private final LedgerAccountLookupPort ledgerAccountLookupPort;
    private final OutboxPort outboxPort;
    private final AdminAuditPort auditPort;
    private final TransactionPort transactionPort;
    private final IdGeneratorPort idGeneratorPort;
    private final LedgerPostingFactory ledgerPostingFactory;
    private final ClockPort clockPort;
    private final BusinessMetricsPort businessMetrics;

    public RetryAdminSettlementService(
            SettlementRepositoryPort settlementRepository,
            WalletRepositoryPort walletRepository,
            LedgerPostingRepositoryPort ledgerPostingRepository,
            LedgerAccountLookupPort ledgerAccountLookupPort,
            OutboxPort outboxPort,
            AdminAuditPort auditPort,
            TransactionPort transactionPort,
            IdGeneratorPort idGeneratorPort,
            LedgerPostingFactory ledgerPostingFactory,
            ClockPort clockPort,
            BusinessMetricsPort businessMetrics
    ) {
        this.settlementRepository = Objects.requireNonNull(settlementRepository);
        this.walletRepository = Objects.requireNonNull(walletRepository);
        this.ledgerPostingRepository = Objects.requireNonNull(ledgerPostingRepository);
        this.ledgerAccountLookupPort = Objects.requireNonNull(ledgerAccountLookupPort);
        this.outboxPort = Objects.requireNonNull(outboxPort);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.transactionPort = Objects.requireNonNull(transactionPort);
        this.idGeneratorPort = Objects.requireNonNull(idGeneratorPort);
        this.ledgerPostingFactory = Objects.requireNonNull(ledgerPostingFactory);
        this.clockPort = Objects.requireNonNull(clockPort);
        this.businessMetrics = Objects.requireNonNull(businessMetrics);
    }

    @Override
    public RetryAdminSettlementResult execute(RetryAdminSettlementCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        RetryAdminSettlementResult result =
                transactionPort.execute(() -> retry(command));

        businessMetrics.recordSettlementRetry(result.status());

        return result;
    }

    private RetryAdminSettlementResult retry(RetryAdminSettlementCommand command) {
        SettlementBatch batch = settlementRepository
                .findByIdForUpdate(new SettlementBatchId(command.batchId()))
                .orElseThrow(() -> new SettlementBatchNotFoundException(command.batchId()));

        batch.prepareRetry();

        List<SettlementBatchItem> retryItems = batch.items().stream()
                .filter(item -> item.status() == SettlementBatchItemStatus.PENDING)
                .toList();

        Map<ShopId, LedgerAccountId> pendingAccounts =
                ledgerAccountLookupPort.sellerPendingAccountsFor(
                        retryItems.stream().map(SettlementBatchItem::shopId).distinct().toList()
                );

        for (SettlementBatchItem item : retryItems) releaseItem(item, pendingAccounts);

        batch.markCompleted();
        settlementRepository.update(batch);
        outboxPort.saveAll(batch.domainEvents());
        batch.clearDomainEvents();

        auditPort.append(
                command.actorUserId(),
                "RETRY_SETTLEMENT_BATCH",
                "SETTLEMENT_BATCH",
                command.batchId(),
                null,
                clockPort.now()
        );

        return new RetryAdminSettlementResult(
                command.batchId(),
                batch.status().name(),
                retryItems.size()
        );
    }

    private void releaseItem(SettlementBatchItem item, Map<ShopId, LedgerAccountId> pendingAccounts) {
        Wallet wallet = walletRepository.findByIdForUpdate(item.walletId())
                .orElseThrow(() -> new WalletNotFoundException(item.walletId()));

        LedgerAccountId pendingAccount = pendingAccounts.get(item.shopId());
        if (pendingAccount == null) {
            throw new IllegalStateException("Seller pending account not found: " + item.shopId().value());
        }

        LedgerAccountId availableAccount = ledgerAccountLookupPort.sellerAvailableAccount(item.shopId());

        wallet.releasePendingToAvailable(item.releasedAmount());

        LedgerPosting posting = ledgerPostingFactory.createSettlementReleasePosting(
                idGeneratorPort.nextLedgerPostingId(),
                item.id(),
                pendingAccount,
                availableAccount,
                item.releasedAmount()
        );

        item.markCompleted(posting.id());
        walletRepository.save(wallet);
        ledgerPostingRepository.save(posting);
    }
}