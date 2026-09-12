package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.ProcessPayoutResultCommand;
import com.taca.paymentwallet.application.exception.PayoutAmountMismatchException;
import com.taca.paymentwallet.application.exception.PayoutNotFoundException;
import com.taca.paymentwallet.application.exception.WalletNotFoundException;
import com.taca.paymentwallet.application.payout.PayoutResultProcessingAction;
import com.taca.paymentwallet.application.payout.PayoutResultStatus;
import com.taca.paymentwallet.application.port.in.ProcessPayoutResultUseCase;
import com.taca.paymentwallet.application.port.out.IdGeneratorPort;
import com.taca.paymentwallet.application.port.out.LedgerAccountLookupPort;
import com.taca.paymentwallet.application.port.out.LedgerPostingRepositoryPort;
import com.taca.paymentwallet.application.port.out.OutboxPort;
import com.taca.paymentwallet.application.port.out.PayoutRepositoryPort;
import com.taca.paymentwallet.application.port.out.TransactionPort;
import com.taca.paymentwallet.application.port.out.WalletRepositoryPort;
import com.taca.paymentwallet.application.result.ProcessPayoutResult;
import com.taca.paymentwallet.domain.payout.Payout;
import com.taca.paymentwallet.domain.payout.PayoutStatus;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PayoutId;
import com.taca.paymentwallet.domain.wallet.LedgerPosting;
import com.taca.paymentwallet.domain.wallet.LedgerPostingFactory;
import com.taca.paymentwallet.domain.wallet.Wallet;

import java.util.Objects;

public class ProcessPayoutResultService implements ProcessPayoutResultUseCase {

    private final PayoutRepositoryPort payoutRepository;
    private final WalletRepositoryPort walletRepository;
    private final LedgerPostingRepositoryPort ledgerPostingRepository;
    private final LedgerAccountLookupPort ledgerAccountLookupPort;
    private final OutboxPort outboxPort;
    private final TransactionPort transactionPort;
    private final IdGeneratorPort idGeneratorPort;
    private final LedgerPostingFactory ledgerPostingFactory;

    public ProcessPayoutResultService(
            PayoutRepositoryPort payoutRepository,
            WalletRepositoryPort walletRepository,
            LedgerPostingRepositoryPort ledgerPostingRepository,
            LedgerAccountLookupPort ledgerAccountLookupPort,
            OutboxPort outboxPort,
            TransactionPort transactionPort,
            IdGeneratorPort idGeneratorPort,
            LedgerPostingFactory ledgerPostingFactory
    ) {
        this.payoutRepository = Objects.requireNonNull(payoutRepository);
        this.walletRepository = Objects.requireNonNull(walletRepository);
        this.ledgerPostingRepository = Objects.requireNonNull(ledgerPostingRepository);
        this.ledgerAccountLookupPort = Objects.requireNonNull(ledgerAccountLookupPort);
        this.outboxPort = Objects.requireNonNull(outboxPort);
        this.transactionPort = Objects.requireNonNull(transactionPort);
        this.idGeneratorPort = Objects.requireNonNull(idGeneratorPort);
        this.ledgerPostingFactory = Objects.requireNonNull(ledgerPostingFactory);
    }

    @Override
    public ProcessPayoutResult execute(ProcessPayoutResultCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return transactionPort.execute(() -> process(command));
    }

    private ProcessPayoutResult process(ProcessPayoutResultCommand command) {
        PayoutId payoutId = new PayoutId(command.payoutId());

        Payout payout = payoutRepository.findByIdForUpdate(payoutId)
                .orElseThrow(() -> new PayoutNotFoundException(payoutId));

        if (isTerminal(payout)) {
            return toResult(payout, PayoutResultProcessingAction.DUPLICATE);
        }

        ensureAmountMatches(payout, new Money(command.amount(), command.currency()));

        if (command.status() == PayoutResultStatus.SUCCESS) {
            payout.markSucceeded(command.providerReference());

            payoutRepository.save(payout);
            outboxPort.saveAll(payout.domainEvents());
            payout.clearDomainEvents();

            return toResult(payout, PayoutResultProcessingAction.APPLIED);
        }

        applyFailedPayout(payout, command.failureCode());

        return toResult(payout, PayoutResultProcessingAction.APPLIED);
    }

    private void applyFailedPayout(Payout payout, String failureCode) {
        Wallet wallet = walletRepository.findByIdForUpdate(payout.walletId())
                .orElseThrow(() -> new WalletNotFoundException(payout.walletId()));

        payout.markFailed(failureCode);
        wallet.reversePayoutReserve(payout.amount());

        LedgerPosting reversalPosting = ledgerPostingFactory.createPayoutReversalPosting(
                idGeneratorPort.nextLedgerPostingId(),
                payout.id(),
                ledgerAccountLookupPort.payoutClearingAccount(),
                ledgerAccountLookupPort.sellerAvailableAccount(payout.shopId()),
                payout.amount()
        );

        walletRepository.save(wallet);
        payoutRepository.save(payout);
        ledgerPostingRepository.save(reversalPosting);

        outboxPort.saveAll(payout.domainEvents());
        payout.clearDomainEvents();
    }

    private boolean isTerminal(Payout payout) {
        return payout.status() == PayoutStatus.SUCCESS
                || payout.status() == PayoutStatus.FAILED
                || payout.status() == PayoutStatus.CANCELLED;
    }

    private void ensureAmountMatches(Payout payout, Money actual) {
        if (!payout.amount().equals(actual)) {
            throw new PayoutAmountMismatchException(
                    payout.id(),
                    payout.amount(),
                    actual
            );
        }
    }

    private ProcessPayoutResult toResult(
            Payout payout,
            PayoutResultProcessingAction action
    ) {
        return new ProcessPayoutResult(
                payout.id().value(),
                payout.walletId().value(),
                payout.status().name(),
                action
        );
    }
}