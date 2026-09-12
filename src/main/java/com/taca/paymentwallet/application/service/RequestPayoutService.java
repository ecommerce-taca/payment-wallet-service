package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.RequestPayoutCommand;
import com.taca.paymentwallet.application.exception.IdempotencyKeyReuseException;
import com.taca.paymentwallet.application.exception.RequestAlreadyProcessingException;
import com.taca.paymentwallet.application.exception.WalletNotFoundException;
import com.taca.paymentwallet.application.idempotency.IdempotencyRecord;
import com.taca.paymentwallet.application.idempotency.IdempotencyScope;
import com.taca.paymentwallet.application.port.in.RequestPayoutUseCase;
import com.taca.paymentwallet.application.port.out.IdGeneratorPort;
import com.taca.paymentwallet.application.port.out.IdempotencyPort;
import com.taca.paymentwallet.application.port.out.LedgerAccountLookupPort;
import com.taca.paymentwallet.application.port.out.LedgerPostingRepositoryPort;
import com.taca.paymentwallet.application.port.out.OutboxPort;
import com.taca.paymentwallet.application.port.out.PayoutRepositoryPort;
import com.taca.paymentwallet.application.port.out.RequestHashPort;
import com.taca.paymentwallet.application.port.out.RequestPayoutResultPayloadPort;
import com.taca.paymentwallet.application.port.out.TransactionPort;
import com.taca.paymentwallet.application.port.out.WalletRepositoryPort;
import com.taca.paymentwallet.application.result.RequestPayoutResult;
import com.taca.paymentwallet.domain.payout.BankAccountSnapshot;
import com.taca.paymentwallet.domain.payout.Payout;
import com.taca.paymentwallet.domain.valueobject.IdempotencyKey;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PayoutId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.wallet.LedgerPosting;
import com.taca.paymentwallet.domain.wallet.LedgerPostingFactory;
import com.taca.paymentwallet.domain.wallet.Wallet;

import java.util.Objects;

public class RequestPayoutService implements RequestPayoutUseCase {

    private final WalletRepositoryPort walletRepository;
    private final PayoutRepositoryPort payoutRepository;
    private final IdempotencyPort idempotencyPort;
    private final RequestHashPort requestHashPort;
    private final IdGeneratorPort idGeneratorPort;
    private final LedgerAccountLookupPort ledgerAccountLookupPort;
    private final LedgerPostingRepositoryPort ledgerPostingRepository;
    private final OutboxPort outboxPort;
    private final TransactionPort transactionPort;
    private final RequestPayoutResultPayloadPort resultPayloadPort;
    private final LedgerPostingFactory ledgerPostingFactory;

    public RequestPayoutService(
            WalletRepositoryPort walletRepository,
            PayoutRepositoryPort payoutRepository,
            IdempotencyPort idempotencyPort,
            RequestHashPort requestHashPort,
            IdGeneratorPort idGeneratorPort,
            LedgerAccountLookupPort ledgerAccountLookupPort,
            LedgerPostingRepositoryPort ledgerPostingRepository,
            OutboxPort outboxPort,
            TransactionPort transactionPort,
            RequestPayoutResultPayloadPort resultPayloadPort,
            LedgerPostingFactory ledgerPostingFactory
    ) {
        this.walletRepository = Objects.requireNonNull(walletRepository);
        this.payoutRepository = Objects.requireNonNull(payoutRepository);
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort);
        this.requestHashPort = Objects.requireNonNull(requestHashPort);
        this.idGeneratorPort = Objects.requireNonNull(idGeneratorPort);
        this.ledgerAccountLookupPort = Objects.requireNonNull(ledgerAccountLookupPort);
        this.ledgerPostingRepository = Objects.requireNonNull(ledgerPostingRepository);
        this.outboxPort = Objects.requireNonNull(outboxPort);
        this.transactionPort = Objects.requireNonNull(transactionPort);
        this.resultPayloadPort = Objects.requireNonNull(resultPayloadPort);
        this.ledgerPostingFactory = Objects.requireNonNull(ledgerPostingFactory);
    }

    @Override
    public RequestPayoutResult execute(RequestPayoutCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        ShopId shopId = new ShopId(command.shopId());
        IdempotencyScope scope = IdempotencyScope.payout(shopId);
        String requestHash = requestHashPort.hash(command);

        return transactionPort.execute(() -> {
            RequestPayoutResult existingResult = resolveExistingIdempotencyResult(
                    scope,
                    command.idempotencyKey(),
                    requestHash
            );

            if (existingResult != null) {
                return existingResult;
            }

            idempotencyPort.reserve(
                    scope,
                    command.idempotencyKey(),
                    requestHash
            );

            RequestPayoutResult result = requestNewPayout(command, shopId);

            idempotencyPort.markSucceeded(
                    scope,
                    command.idempotencyKey(),
                    resultPayloadPort.serialize(result)
            );

            return result;
        });
    }

    private RequestPayoutResult resolveExistingIdempotencyResult(
            IdempotencyScope scope,
            String idempotencyKey,
            String requestHash
    ) {
        return idempotencyPort.find(scope, idempotencyKey)
                .map(record -> resolveExistingRecord(record, idempotencyKey, requestHash))
                .orElse(null);
    }

    private RequestPayoutResult resolveExistingRecord(
            IdempotencyRecord record,
            String idempotencyKey,
            String requestHash
    ) {
        if (!record.requestHash().equals(requestHash)) {
            throw new IdempotencyKeyReuseException(idempotencyKey);
        }

        if (record.isProcessing()) {
            throw new RequestAlreadyProcessingException(idempotencyKey);
        }

        if (record.isSucceeded()) {
            return resultPayloadPort.deserialize(record.responsePayload());
        }

        return null;
    }

    private RequestPayoutResult requestNewPayout(
            RequestPayoutCommand command,
            ShopId shopId
    ) {
        Money amount = new Money(command.amount(), command.currency());

        Wallet wallet = walletRepository.findByShopIdAndCurrencyForUpdate(
                        shopId,
                        command.currency()
                )
                .orElseThrow(() -> new WalletNotFoundException(shopId, command.currency()));

        wallet.reservePayout(amount);

        PayoutId payoutId = idGeneratorPort.nextPayoutId();

        Payout payout = Payout.request(
                payoutId,
                wallet.id(),
                shopId,
                amount,
                new BankAccountSnapshot(
                        command.bankCode(),
                        command.accountHolderName(),
                        command.maskedAccountNumber()
                ),
                new IdempotencyKey(command.idempotencyKey())
        );

        LedgerPosting posting = ledgerPostingFactory.createPayoutReservePosting(
                idGeneratorPort.nextLedgerPostingId(),
                payout.id(),
                ledgerAccountLookupPort.sellerAvailableAccount(shopId),
                ledgerAccountLookupPort.payoutClearingAccount(),
                amount
        );

        walletRepository.save(wallet);
        payoutRepository.save(payout);
        ledgerPostingRepository.save(posting);
        outboxPort.saveAll(payout.domainEvents());
        payout.clearDomainEvents();

        return new RequestPayoutResult(
                payout.id().value(),
                wallet.id().value(),
                shopId.value(),
                payout.amount().amount(),
                payout.amount().currency(),
                payout.status().name()
        );
    }
}