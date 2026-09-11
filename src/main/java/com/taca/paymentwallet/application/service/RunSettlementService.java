package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.RunSettlementCommand;
import com.taca.paymentwallet.application.port.in.RunSettlementUseCase;
import com.taca.paymentwallet.application.port.out.IdGeneratorPort;
import com.taca.paymentwallet.application.port.out.LedgerAccountLookupPort;
import com.taca.paymentwallet.application.port.out.LedgerPostingRepositoryPort;
import com.taca.paymentwallet.application.port.out.OutboxPort;
import com.taca.paymentwallet.application.port.out.SettlementCandidatePort;
import com.taca.paymentwallet.application.port.out.SettlementRepositoryPort;
import com.taca.paymentwallet.application.port.out.TransactionPort;
import com.taca.paymentwallet.application.port.out.WalletRepositoryPort;
import com.taca.paymentwallet.application.result.RunSettlementResult;
import com.taca.paymentwallet.application.settlement.SettlementCandidate;
import com.taca.paymentwallet.application.settlement.SettlementRunStatus;
import com.taca.paymentwallet.domain.settlement.SettlementBatch;
import com.taca.paymentwallet.domain.settlement.SettlementBatchItem;
import com.taca.paymentwallet.domain.settlement.SettlementLine;
import com.taca.paymentwallet.domain.valueobject.LedgerAccountId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;
import com.taca.paymentwallet.domain.wallet.LedgerPosting;
import com.taca.paymentwallet.domain.wallet.LedgerPostingFactory;
import com.taca.paymentwallet.domain.wallet.Wallet;
import com.taca.paymentwallet.application.exception.WalletNotFoundException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RunSettlementService implements RunSettlementUseCase {

    private final SettlementCandidatePort settlementCandidatePort;
    private final SettlementRepositoryPort settlementRepository;
    private final WalletRepositoryPort walletRepository;
    private final LedgerPostingRepositoryPort ledgerPostingRepository;
    private final LedgerAccountLookupPort ledgerAccountLookupPort;
    private final OutboxPort outboxPort;
    private final TransactionPort transactionPort;
    private final IdGeneratorPort idGeneratorPort;
    private final LedgerPostingFactory ledgerPostingFactory;

    public RunSettlementService(
            SettlementCandidatePort settlementCandidatePort,
            SettlementRepositoryPort settlementRepository,
            WalletRepositoryPort walletRepository,
            LedgerPostingRepositoryPort ledgerPostingRepository,
            LedgerAccountLookupPort ledgerAccountLookupPort,
            OutboxPort outboxPort,
            TransactionPort transactionPort,
            IdGeneratorPort idGeneratorPort,
            LedgerPostingFactory ledgerPostingFactory
    ) {
        this.settlementCandidatePort = Objects.requireNonNull(settlementCandidatePort);
        this.settlementRepository = Objects.requireNonNull(settlementRepository);
        this.walletRepository = Objects.requireNonNull(walletRepository);
        this.ledgerPostingRepository = Objects.requireNonNull(ledgerPostingRepository);
        this.ledgerAccountLookupPort = Objects.requireNonNull(ledgerAccountLookupPort);
        this.outboxPort = Objects.requireNonNull(outboxPort);
        this.transactionPort = Objects.requireNonNull(transactionPort);
        this.idGeneratorPort = Objects.requireNonNull(idGeneratorPort);
        this.ledgerPostingFactory = Objects.requireNonNull(ledgerPostingFactory);
    }

    @Override
    public RunSettlementResult execute(RunSettlementCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return transactionPort.execute(() -> run(command));
    }

    private RunSettlementResult run(RunSettlementCommand command) {
        List<SettlementCandidate> candidates =
                settlementCandidatePort.findEligibleCandidates(
                        command.periodStart(),
                        command.periodEnd()
                );

        if (candidates.isEmpty()) {
            return RunSettlementResult.empty();
        }

        List<SettlementBatchItem> items = buildItems(candidates);

        SettlementBatch batch = new SettlementBatch(
                idGeneratorPort.nextSettlementBatchId(),
                command.periodStart(),
                command.periodEnd(),
                items
        );

        batch.markProcessing();

        Map<ShopId, LedgerAccountId> sellerPendingAccounts =
                ledgerAccountLookupPort.sellerPendingAccountsFor(
                        items.stream()
                                .map(SettlementBatchItem::shopId)
                                .distinct()
                                .toList()
                );

        for (SettlementBatchItem item : batch.items()) {
            releaseItem(item, sellerPendingAccounts);
        }

        batch.markCompleted();
        settlementRepository.save(batch);

        outboxPort.saveAll(batch.domainEvents());
        batch.clearDomainEvents();

        return new RunSettlementResult(
                batch.id().value(),
                SettlementRunStatus.COMPLETED,
                batch.items().size(),
                batch.totalReleased().amount(),
                batch.totalReleased().currency()
        );
    }

    private List<SettlementBatchItem> buildItems(List<SettlementCandidate> candidates) {
        Map<SettlementGroupKey, List<SettlementCandidate>> candidatesByWallet =
                new LinkedHashMap<>();

        for (SettlementCandidate candidate : candidates) {
            SettlementGroupKey key = new SettlementGroupKey(
                    candidate.shopId(),
                    candidate.walletId()
            );

            candidatesByWallet.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(candidate);
        }

        List<SettlementBatchItem> items = new ArrayList<>();

        for (Map.Entry<SettlementGroupKey, List<SettlementCandidate>> entry
                : candidatesByWallet.entrySet()) {
            items.add(buildItem(entry.getKey(), entry.getValue()));
        }

        return items;
    }

    private SettlementBatchItem buildItem(
            SettlementGroupKey key,
            List<SettlementCandidate> candidates
    ) {
        List<SettlementLine> lines = candidates.stream()
                .map(candidate -> new SettlementLine(
                        idGeneratorPort.nextSettlementLineId(),
                        candidate.paymentAllocationId(),
                        candidate.releasableAmount()
                ))
                .toList();

        Money gross = candidates.stream()
                .map(SettlementCandidate::grossAmount)
                .reduce(Money.vnd(0), Money::add);

        Money commission = candidates.stream()
                .map(SettlementCandidate::commissionAmount)
                .reduce(Money.vnd(0), Money::add);

        Money tax = candidates.stream()
                .map(SettlementCandidate::taxAmount)
                .reduce(Money.vnd(0), Money::add);

        Money net = candidates.stream()
                .map(SettlementCandidate::sellerNetAmount)
                .reduce(Money.vnd(0), Money::add);

        Money releasedAmount = candidates.stream()
                .map(SettlementCandidate::releasableAmount)
                .reduce(Money.vnd(0), Money::add);

        Money heldAmount = candidates.stream()
                .map(SettlementCandidate::heldAmount)
                .reduce(Money.vnd(0), Money::add);

        return new SettlementBatchItem(
                idGeneratorPort.nextSettlementBatchItemId(),
                key.shopId(),
                key.walletId(),
                gross,
                commission,
                tax,
                net,
                releasedAmount,
                heldAmount,
                lines
        );
    }

    private void releaseItem(
            SettlementBatchItem item,
            Map<ShopId, LedgerAccountId> sellerPendingAccounts
    ) {
        Wallet wallet = walletRepository.findByIdForUpdate(item.walletId())
                .orElseThrow(() -> new WalletNotFoundException(item.walletId()));

        LedgerAccountId sellerPendingAccountId =
                sellerPendingAccounts.get(item.shopId());

        if (sellerPendingAccountId == null) {
            throw new IllegalStateException("Seller pending account not found: "
                    + item.shopId().value());
        }

        LedgerAccountId sellerAvailableAccountId =
                ledgerAccountLookupPort.sellerAvailableAccount(item.shopId());

        wallet.releasePendingToAvailable(item.releasedAmount());

        LedgerPosting posting = ledgerPostingFactory.createSettlementReleasePosting(
                idGeneratorPort.nextLedgerPostingId(),
                item.id(),
                sellerPendingAccountId,
                sellerAvailableAccountId,
                item.releasedAmount()
        );

        item.markCompleted(posting.id());

        walletRepository.save(wallet);
        ledgerPostingRepository.save(posting);
    }

    private record SettlementGroupKey(
            ShopId shopId,
            WalletId walletId
    ) {
    }
}
