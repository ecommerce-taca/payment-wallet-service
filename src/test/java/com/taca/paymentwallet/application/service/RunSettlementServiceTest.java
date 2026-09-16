package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.RunSettlementCommand;
import com.taca.paymentwallet.application.exception.WalletNotFoundException;
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
import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.settlement.SettlementBatch;
import com.taca.paymentwallet.domain.settlement.SettlementBatchItemStatus;
import com.taca.paymentwallet.domain.settlement.SettlementBatchStatus;
import com.taca.paymentwallet.domain.valueobject.BuyerUserId;
import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.LedgerAccountId;
import com.taca.paymentwallet.domain.valueobject.LedgerPostingId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.PayoutId;
import com.taca.paymentwallet.domain.valueobject.RefundId;
import com.taca.paymentwallet.domain.valueobject.SettlementBatchId;
import com.taca.paymentwallet.domain.valueobject.SettlementBatchItemId;
import com.taca.paymentwallet.domain.valueobject.SettlementLineId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;
import com.taca.paymentwallet.domain.wallet.LedgerPosting;
import com.taca.paymentwallet.domain.wallet.LedgerPostingFactory;
import com.taca.paymentwallet.domain.wallet.Wallet;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RunSettlementServiceTest {

    private final FakeSettlementCandidatePort settlementCandidatePort =
            new FakeSettlementCandidatePort();

    private final FakeSettlementRepositoryPort settlementRepository =
            new FakeSettlementRepositoryPort();

    private final FakeWalletRepositoryPort walletRepository =
            new FakeWalletRepositoryPort();

    private final FakeLedgerPostingRepositoryPort ledgerPostingRepository =
            new FakeLedgerPostingRepositoryPort();

    private final FakeLedgerAccountLookupPort ledgerAccountLookupPort =
            new FakeLedgerAccountLookupPort();

    private final FakeOutboxPort outboxPort =
            new FakeOutboxPort();

    private final FakeTransactionPort transactionPort =
            new FakeTransactionPort();

    private final FakeIdGeneratorPort idGeneratorPort =
            new FakeIdGeneratorPort();

    private final RunSettlementService service = new RunSettlementService(
            settlementCandidatePort,
            settlementRepository,
            walletRepository,
            ledgerPostingRepository,
            ledgerAccountLookupPort,
            outboxPort,
            transactionPort,
            idGeneratorPort,
            new LedgerPostingFactory()
    );

    @Test
    void shouldReturnEmptyWhenNoCandidate() {
        RunSettlementResult result = service.execute(new RunSettlementCommand(
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-02T00:00:00Z")
        ));

        assertThat(result.status()).isEqualTo(SettlementRunStatus.EMPTY);
        assertThat(result.settlementBatchId()).isNull();
        assertThat(result.itemCount()).isZero();
        assertThat(result.totalReleasedAmount()).isZero();
        assertThat(result.currency()).isEqualTo("VND");

        assertThat(settlementRepository.savedBatch).isNull();
        assertThat(ledgerPostingRepository.postings).isEmpty();
        assertThat(outboxPort.events).isEmpty();
    }

    @Test
    void shouldReleaseOneSettlementCandidate() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        WalletId walletId = new WalletId(UUID.randomUUID());

        SettlementCandidate candidate = candidate(
                shopId,
                walletId,
                Money.vnd(100_000),
                Money.vnd(7_000),
                Money.vnd(1_000),
                Money.vnd(92_000),
                Money.vnd(92_000),
                Money.vnd(0)
        );

        settlementCandidatePort.candidates.add(candidate);
        walletRepository.add(walletWithPending(walletId, shopId, Money.vnd(92_000)));

        RunSettlementResult result = service.execute(new RunSettlementCommand(
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-02T00:00:00Z")
        ));

        Wallet wallet = walletRepository.wallets.get(walletId);

        assertThat(result.status()).isEqualTo(SettlementRunStatus.COMPLETED);
        assertThat(result.settlementBatchId()).isNotNull();
        assertThat(result.itemCount()).isEqualTo(1);
        assertThat(result.totalReleasedAmount()).isEqualTo(92_000);
        assertThat(result.currency()).isEqualTo("VND");

        assertThat(wallet.pendingBalance()).isEqualTo(Money.vnd(0));
        assertThat(wallet.availableBalance()).isEqualTo(Money.vnd(92_000));

        assertThat(ledgerPostingRepository.postings).hasSize(1);
        assertThat(settlementRepository.savedBatch).isNotNull();
        assertThat(settlementRepository.savedBatch.status())
                .isEqualTo(SettlementBatchStatus.COMPLETED);
        assertThat(settlementRepository.savedBatch.items()).hasSize(1);
        assertThat(settlementRepository.savedBatch.items().getFirst().status())
                .isEqualTo(SettlementBatchItemStatus.COMPLETED);
        assertThat(outboxPort.events).hasSize(1);
    }

    @Test
    void shouldGroupCandidatesBySameShopAndWallet() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        WalletId walletId = new WalletId(UUID.randomUUID());

        settlementCandidatePort.candidates.add(candidate(
                shopId,
                walletId,
                Money.vnd(100_000),
                Money.vnd(7_000),
                Money.vnd(1_000),
                Money.vnd(92_000),
                Money.vnd(92_000),
                Money.vnd(0)
        ));

        settlementCandidatePort.candidates.add(candidate(
                shopId,
                walletId,
                Money.vnd(50_000),
                Money.vnd(3_500),
                Money.vnd(500),
                Money.vnd(46_000),
                Money.vnd(46_000),
                Money.vnd(0)
        ));

        walletRepository.add(walletWithPending(walletId, shopId, Money.vnd(138_000)));

        RunSettlementResult result = service.execute(new RunSettlementCommand(
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-02T00:00:00Z")
        ));

        Wallet wallet = walletRepository.wallets.get(walletId);

        assertThat(result.status()).isEqualTo(SettlementRunStatus.COMPLETED);
        assertThat(result.itemCount()).isEqualTo(1);
        assertThat(result.totalReleasedAmount()).isEqualTo(138_000);

        assertThat(wallet.pendingBalance()).isEqualTo(Money.vnd(0));
        assertThat(wallet.availableBalance()).isEqualTo(Money.vnd(138_000));

        assertThat(settlementRepository.savedBatch.items()).hasSize(1);
        assertThat(settlementRepository.savedBatch.items().getFirst().lines()).hasSize(2);
        assertThat(ledgerPostingRepository.postings).hasSize(1);
    }

    @Test
    void shouldCreateSeparateItemsForDifferentWallets() {
        ShopId firstShopId = new ShopId(UUID.randomUUID());
        WalletId firstWalletId = new WalletId(UUID.randomUUID());

        ShopId secondShopId = new ShopId(UUID.randomUUID());
        WalletId secondWalletId = new WalletId(UUID.randomUUID());

        settlementCandidatePort.candidates.add(candidate(
                firstShopId,
                firstWalletId,
                Money.vnd(100_000),
                Money.vnd(7_000),
                Money.vnd(1_000),
                Money.vnd(92_000),
                Money.vnd(92_000),
                Money.vnd(0)
        ));

        settlementCandidatePort.candidates.add(candidate(
                secondShopId,
                secondWalletId,
                Money.vnd(50_000),
                Money.vnd(3_500),
                Money.vnd(500),
                Money.vnd(46_000),
                Money.vnd(46_000),
                Money.vnd(0)
        ));

        walletRepository.add(walletWithPending(firstWalletId, firstShopId, Money.vnd(92_000)));
        walletRepository.add(walletWithPending(secondWalletId, secondShopId, Money.vnd(46_000)));

        RunSettlementResult result = service.execute(new RunSettlementCommand(
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-02T00:00:00Z")
        ));

        assertThat(result.status()).isEqualTo(SettlementRunStatus.COMPLETED);
        assertThat(result.itemCount()).isEqualTo(2);
        assertThat(result.totalReleasedAmount()).isEqualTo(138_000);

        assertThat(settlementRepository.savedBatch.items()).hasSize(2);
        assertThat(ledgerPostingRepository.postings).hasSize(2);
        assertThat(outboxPort.events).hasSize(1);
    }

    @Test
    void shouldRejectWhenWalletNotFound() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        WalletId walletId = new WalletId(UUID.randomUUID());

        settlementCandidatePort.candidates.add(candidate(
                shopId,
                walletId,
                Money.vnd(100_000),
                Money.vnd(7_000),
                Money.vnd(1_000),
                Money.vnd(92_000),
                Money.vnd(92_000),
                Money.vnd(0)
        ));

        assertThatThrownBy(() -> service.execute(new RunSettlementCommand(
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-02T00:00:00Z")
        ))).isInstanceOf(WalletNotFoundException.class);
    }

    private SettlementCandidate candidate(
            ShopId shopId,
            WalletId walletId,
            Money grossAmount,
            Money commissionAmount,
            Money taxAmount,
            Money sellerNetAmount,
            Money releasableAmount,
            Money heldAmount
    ) {
        return new SettlementCandidate(
                new PaymentAllocationId(UUID.randomUUID()),
                shopId,
                walletId,
                grossAmount,
                commissionAmount,
                taxAmount,
                sellerNetAmount,
                releasableAmount,
                heldAmount
        );
    }

    private Wallet walletWithPending(
            WalletId walletId,
            ShopId shopId,
            Money pendingAmount
    ) {
        Wallet wallet = Wallet.create(walletId, shopId);
        wallet.creditPending(pendingAmount);
        return wallet;
    }

    private static class FakeSettlementCandidatePort implements SettlementCandidatePort {

        private final List<SettlementCandidate> candidates = new ArrayList<>();

        @Override
        public List<SettlementCandidate> findEligibleCandidates(
                Instant periodStart,
                Instant periodEnd
        ) {
            return candidates;
        }
    }

    private static class FakeSettlementRepositoryPort implements SettlementRepositoryPort {

        private SettlementBatch savedBatch;

        @Override
        public SettlementBatch save(SettlementBatch settlementBatch) {
            this.savedBatch = settlementBatch;
            return settlementBatch;
        }
    }

    private static class FakeWalletRepositoryPort implements WalletRepositoryPort {

        private final Map<WalletId, Wallet> wallets = new HashMap<>();

        void add(Wallet wallet) {
            wallets.put(wallet.id(), wallet);
        }

        @Override
        public Optional<Wallet> findById(WalletId walletId) {
            return Optional.ofNullable(wallets.get(walletId));
        }

        @Override
        public Optional<Wallet> findByIdForUpdate(WalletId walletId) {
            return Optional.ofNullable(wallets.get(walletId));
        }

        @Override
        public Optional<Wallet> findByShopIdAndCurrencyForUpdate(
                ShopId shopId,
                String currency
        ) {
            return wallets.values().stream()
                    .filter(wallet -> wallet.shopId().equals(shopId))
                    .filter(wallet -> wallet.currency().equals(currency))
                    .findFirst();
        }

        @Override
        public Wallet save(Wallet wallet) {
            wallets.put(wallet.id(), wallet);
            return wallet;
        }
    }

    private static class FakeLedgerPostingRepositoryPort
            implements LedgerPostingRepositoryPort {

        private final List<LedgerPosting> postings = new ArrayList<>();

        @Override
        public void save(LedgerPosting posting) {
            postings.add(posting);
        }
    }

    private static class FakeOutboxPort implements OutboxPort {

        private final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void save(DomainEvent event) {
            events.add(event);
        }
    }

    private static class FakeTransactionPort implements TransactionPort {

        @Override
        public <T> T execute(Supplier<T> action) {
            return action.get();
        }
    }

    private static class FakeLedgerAccountLookupPort implements LedgerAccountLookupPort {

        @Override
        public LedgerAccountId vnpayClearingAccount() {
            return new LedgerAccountId(UUID.randomUUID());
        }

        @Override
        public LedgerAccountId platformCommissionAccount() {
            return new LedgerAccountId(UUID.randomUUID());
        }

        @Override
        public LedgerAccountId taxPayableAccount() {
            return new LedgerAccountId(UUID.randomUUID());
        }

        @Override
        public Map<ShopId, LedgerAccountId> sellerPendingAccountsFor(List<ShopId> shopIds) {
            Map<ShopId, LedgerAccountId> result = new HashMap<>();

            for (ShopId shopId : shopIds) {
                result.put(shopId, new LedgerAccountId(UUID.randomUUID()));
            }

            return result;
        }

        @Override
        public LedgerAccountId refundClearingAccount() {
            return new LedgerAccountId(UUID.randomUUID());
        }

        @Override
        public Map<PaymentAllocationId, LedgerAccountId> sellerRefundAccountsFor(
                List<PaymentAllocationId> paymentAllocationIds
        ) {
            Map<PaymentAllocationId, LedgerAccountId> result = new HashMap<>();

            for (PaymentAllocationId paymentAllocationId : paymentAllocationIds) {
                result.put(paymentAllocationId, new LedgerAccountId(UUID.randomUUID()));
            }

            return result;
        }

        @Override
        public LedgerAccountId sellerAvailableAccount(ShopId shopId) {
            return new LedgerAccountId(UUID.randomUUID());
        }

        @Override
        public LedgerAccountId payoutClearingAccount() {
            return new LedgerAccountId(UUID.randomUUID());
        }

        @Override
        public LedgerAccountId codClearingAccount() {
            return new LedgerAccountId(UUID.randomUUID());
        }
    }

    private static class FakeIdGeneratorPort implements IdGeneratorPort {

        @Override
        public PaymentId nextPaymentId() {
            return new PaymentId(UUID.randomUUID());
        }

        @Override
        public PaymentAllocationId nextPaymentAllocationId() {
            return new PaymentAllocationId(UUID.randomUUID());
        }

        @Override
        public WalletId nextWalletId() {
            return new WalletId(UUID.randomUUID());
        }

        @Override
        public LedgerAccountId nextLedgerAccountId() {
            return new LedgerAccountId(UUID.randomUUID());
        }

        @Override
        public LedgerPostingId nextLedgerPostingId() {
            return new LedgerPostingId(UUID.randomUUID());
        }

        @Override
        public RefundId nextRefundId() {
            return new RefundId(UUID.randomUUID());
        }

        @Override
        public PayoutId nextPayoutId() {
            return new PayoutId(UUID.randomUUID());
        }

        @Override
        public SettlementBatchId nextSettlementBatchId() {
            return new SettlementBatchId(UUID.randomUUID());
        }

        @Override
        public SettlementBatchItemId nextSettlementBatchItemId() {
            return new SettlementBatchItemId(UUID.randomUUID());
        }

        @Override
        public SettlementLineId nextSettlementLineId() {
            return new SettlementLineId(UUID.randomUUID());
        }
    }
}