package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.CreatePaymentCommand;
import com.taca.paymentwallet.application.command.RequestPayoutCommand;
import com.taca.paymentwallet.application.command.RequestRefundCommand;
import com.taca.paymentwallet.application.exception.IdempotencyKeyReuseException;
import com.taca.paymentwallet.application.exception.RequestAlreadyProcessingException;
import com.taca.paymentwallet.application.exception.WalletNotFoundException;
import com.taca.paymentwallet.application.idempotency.IdempotencyRecord;
import com.taca.paymentwallet.application.idempotency.IdempotencyScope;
import com.taca.paymentwallet.application.idempotency.IdempotencyStatus;
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
import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.payout.Payout;
import com.taca.paymentwallet.domain.payout.PayoutStatus;
import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.LedgerAccountId;
import com.taca.paymentwallet.domain.valueobject.LedgerPostingId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.PayoutId;
import com.taca.paymentwallet.domain.valueobject.RefundId;
import com.taca.paymentwallet.domain.valueobject.SettlementBatchId;
import com.taca.paymentwallet.domain.valueobject.SettlementBatchItemId;
import com.taca.paymentwallet.domain.valueobject.SettlementLineId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;
import com.taca.paymentwallet.domain.wallet.InsufficientWalletBalanceException;
import com.taca.paymentwallet.domain.wallet.LedgerPosting;
import com.taca.paymentwallet.domain.wallet.LedgerPostingFactory;
import com.taca.paymentwallet.domain.wallet.Wallet;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestPayoutServiceTest {

    @Test
    void shouldRequestPayout() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        WalletId walletId = new WalletId(UUID.randomUUID());
        PayoutId payoutId = new PayoutId(UUID.randomUUID());

        Wallet wallet = activeWalletWithAvailableBalance(
                walletId,
                shopId,
                Money.vnd(100_000)
        );

        FakeWalletRepositoryPort walletRepository =
                new FakeWalletRepositoryPort(wallet);

        FakePayoutRepositoryPort payoutRepository =
                new FakePayoutRepositoryPort();

        FakeIdempotencyPort idempotencyPort =
                new FakeIdempotencyPort();

        FakeLedgerPostingRepositoryPort ledgerPostingRepository =
                new FakeLedgerPostingRepositoryPort();

        FakeOutboxPort outboxPort =
                new FakeOutboxPort();

        RequestPayoutService service = newService(
                walletRepository,
                payoutRepository,
                idempotencyPort,
                ledgerPostingRepository,
                outboxPort,
                new FixedIdGeneratorPort(payoutId)
        );

        RequestPayoutResult result = service.execute(
                payoutCommand(shopId.value(), 50_000)
        );

        assertEquals(payoutId.value(), result.payoutId());
        assertEquals(walletId.value(), result.walletId());
        assertEquals(shopId.value(), result.shopId());
        assertEquals(50_000, result.amount());
        assertEquals("VND", result.currency());
        assertEquals("REQUESTED", result.status());

        assertEquals(Money.vnd(50_000), walletRepository.wallet.availableBalance());
        assertEquals(1, walletRepository.savedWallets.size());

        assertEquals(1, payoutRepository.savedPayouts.size());
        assertEquals(PayoutStatus.REQUESTED, payoutRepository.savedPayouts.getFirst().status());

        assertEquals(1, ledgerPostingRepository.savedPostings.size());
        assertEquals("PAYOUT_RESERVE", ledgerPostingRepository.savedPostings.getFirst().postingType());

        assertEquals(IdempotencyStatus.SUCCEEDED, idempotencyPort.record.status());

        assertTrue(outboxPort.events.stream()
                .anyMatch(event -> event.eventType().equals("payout.requested")));
    }

    @Test
    void shouldReturnPreviousResultWhenIdempotencySucceeded() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        WalletId existingWalletId = new WalletId(UUID.randomUUID());
        PayoutId existingPayoutId = new PayoutId(UUID.randomUUID());

        FakeIdempotencyPort idempotencyPort = new FakeIdempotencyPort();
        idempotencyPort.record = new IdempotencyRecord(
                IdempotencyScope.payout(shopId),
                "payout-idem-key-1",
                "payout-request-hash",
                IdempotencyStatus.SUCCEEDED,
                existingPayoutId.value()
                        + "|"
                        + existingWalletId.value()
                        + "|"
                        + shopId.value()
                        + "|50000|VND|REQUESTED",
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );

        FakeWalletRepositoryPort walletRepository =
                new FakeWalletRepositoryPort(activeWalletWithAvailableBalance(
                        existingWalletId,
                        shopId,
                        Money.vnd(100_000)
                ));

        FakePayoutRepositoryPort payoutRepository =
                new FakePayoutRepositoryPort();

        FakeLedgerPostingRepositoryPort ledgerPostingRepository =
                new FakeLedgerPostingRepositoryPort();

        FakeOutboxPort outboxPort =
                new FakeOutboxPort();

        RequestPayoutService service = newService(
                walletRepository,
                payoutRepository,
                idempotencyPort,
                ledgerPostingRepository,
                outboxPort,
                new FixedIdGeneratorPort(new PayoutId(UUID.randomUUID()))
        );

        RequestPayoutResult result = service.execute(
                payoutCommand(shopId.value(), 50_000)
        );

        assertEquals(existingPayoutId.value(), result.payoutId());
        assertEquals(existingWalletId.value(), result.walletId());
        assertEquals(shopId.value(), result.shopId());
        assertEquals(50_000, result.amount());
        assertEquals("REQUESTED", result.status());

        assertEquals(0, walletRepository.savedWallets.size());
        assertEquals(0, payoutRepository.savedPayouts.size());
        assertEquals(0, ledgerPostingRepository.savedPostings.size());
        assertEquals(0, outboxPort.events.size());
    }

    @Test
    void shouldRejectSameIdempotencyKeyWithDifferentRequestHash() {
        ShopId shopId = new ShopId(UUID.randomUUID());

        FakeIdempotencyPort idempotencyPort = new FakeIdempotencyPort();
        idempotencyPort.record = new IdempotencyRecord(
                IdempotencyScope.payout(shopId),
                "payout-idem-key-1",
                "different-request-hash",
                IdempotencyStatus.SUCCEEDED,
                UUID.randomUUID()
                        + "|"
                        + UUID.randomUUID()
                        + "|"
                        + shopId.value()
                        + "|50000|VND|REQUESTED",
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );

        RequestPayoutService service = newService(
                new FakeWalletRepositoryPort(activeWalletWithAvailableBalance(
                        new WalletId(UUID.randomUUID()),
                        shopId,
                        Money.vnd(100_000)
                )),
                new FakePayoutRepositoryPort(),
                idempotencyPort,
                new FakeLedgerPostingRepositoryPort(),
                new FakeOutboxPort(),
                new FixedIdGeneratorPort(new PayoutId(UUID.randomUUID()))
        );

        assertThrows(
                IdempotencyKeyReuseException.class,
                () -> service.execute(payoutCommand(shopId.value(), 50_000))
        );
    }

    @Test
    void shouldRejectProcessingIdempotencyRequest() {
        ShopId shopId = new ShopId(UUID.randomUUID());

        FakeIdempotencyPort idempotencyPort = new FakeIdempotencyPort();
        idempotencyPort.record = new IdempotencyRecord(
                IdempotencyScope.payout(shopId),
                "payout-idem-key-1",
                "payout-request-hash",
                IdempotencyStatus.PROCESSING,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );

        RequestPayoutService service = newService(
                new FakeWalletRepositoryPort(activeWalletWithAvailableBalance(
                        new WalletId(UUID.randomUUID()),
                        shopId,
                        Money.vnd(100_000)
                )),
                new FakePayoutRepositoryPort(),
                idempotencyPort,
                new FakeLedgerPostingRepositoryPort(),
                new FakeOutboxPort(),
                new FixedIdGeneratorPort(new PayoutId(UUID.randomUUID()))
        );

        assertThrows(
                RequestAlreadyProcessingException.class,
                () -> service.execute(payoutCommand(shopId.value(), 50_000))
        );
    }

    @Test
    void shouldRejectUnknownWallet() {
        ShopId shopId = new ShopId(UUID.randomUUID());

        RequestPayoutService service = newService(
                new FakeWalletRepositoryPort(null),
                new FakePayoutRepositoryPort(),
                new FakeIdempotencyPort(),
                new FakeLedgerPostingRepositoryPort(),
                new FakeOutboxPort(),
                new FixedIdGeneratorPort(new PayoutId(UUID.randomUUID()))
        );

        assertThrows(
                WalletNotFoundException.class,
                () -> service.execute(payoutCommand(shopId.value(), 50_000))
        );
    }

    @Test
    void shouldRejectPayoutWhenAvailableBalanceIsInsufficient() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        WalletId walletId = new WalletId(UUID.randomUUID());

        Wallet wallet = Wallet.create(walletId, shopId);

        RequestPayoutService service = newService(
                new FakeWalletRepositoryPort(wallet),
                new FakePayoutRepositoryPort(),
                new FakeIdempotencyPort(),
                new FakeLedgerPostingRepositoryPort(),
                new FakeOutboxPort(),
                new FixedIdGeneratorPort(new PayoutId(UUID.randomUUID()))
        );

        assertThrows(
                InsufficientWalletBalanceException.class,
                () -> service.execute(payoutCommand(shopId.value(), 50_000))
        );
    }

    private RequestPayoutService newService(
            FakeWalletRepositoryPort walletRepository,
            FakePayoutRepositoryPort payoutRepository,
            FakeIdempotencyPort idempotencyPort,
            FakeLedgerPostingRepositoryPort ledgerPostingRepository,
            FakeOutboxPort outboxPort,
            IdGeneratorPort idGeneratorPort
    ) {
        return new RequestPayoutService(
                walletRepository,
                payoutRepository,
                idempotencyPort,
                new FakeRequestHashPort(),
                idGeneratorPort,
                new FakeLedgerAccountLookupPort(),
                ledgerPostingRepository,
                outboxPort,
                new ImmediateTransactionPort(),
                new PipeSeparatedRequestPayoutResultPayloadPort(),
                new LedgerPostingFactory()
        );
    }

    private Wallet activeWalletWithAvailableBalance(
            WalletId walletId,
            ShopId shopId,
            Money amount
    ) {
        Wallet wallet = Wallet.create(walletId, shopId);
        wallet.creditPending(amount);
        wallet.releasePendingToAvailable(amount);
        return wallet;
    }

    private RequestPayoutCommand payoutCommand(UUID shopId, long amount) {
        return new RequestPayoutCommand(
                shopId,
                amount,
                "VND",
                "VCB",
                "NGUYEN VAN A",
                "********1234",
                "payout-idem-key-1"
        );
    }

    private static final class FakeWalletRepositoryPort implements WalletRepositoryPort {

        private Wallet wallet;
        private final List<Wallet> savedWallets = new ArrayList<>();
        private final Map<WalletId, Wallet> walletsById = new HashMap<>();

        private FakeWalletRepositoryPort(Wallet wallet) {
            this.wallet = wallet;
        }

        @Override
        public Optional<Wallet> findById(WalletId walletId) {
            return Optional.ofNullable(wallet);
        }

        @Override
        public Optional<Wallet> findByShopIdAndCurrencyForUpdate(
                ShopId shopId,
                String currency
        ) {
            if (wallet == null) {
                return Optional.empty();
            }

            if (!wallet.shopId().equals(shopId)) {
                return Optional.empty();
            }

            if (!wallet.currency().equals(currency)) {
                return Optional.empty();
            }

            return Optional.of(wallet);
        }

        @Override
        public Wallet save(Wallet wallet) {
            this.wallet = wallet;
            this.savedWallets.add(wallet);
            return wallet;
        }

        @Override
        public Optional<Wallet> findByIdForUpdate(WalletId walletId) {
            return Optional.ofNullable(walletsById.get(walletId));
        }
    }

    private static final class FakePayoutRepositoryPort implements PayoutRepositoryPort {

        private final List<Payout> savedPayouts = new ArrayList<>();

        @Override
        public Optional<Payout> findById(PayoutId payoutId) {
            return Optional.empty();
        }

        @Override
        public Optional<Payout> findByIdForUpdate(PayoutId payoutId) {
            return Optional.empty();
        }

        @Override
        public Payout save(Payout payout) {
            savedPayouts.add(payout);
            return payout;
        }
    }

    private static final class FakeIdempotencyPort implements IdempotencyPort {

        private IdempotencyRecord record;

        @Override
        public Optional<IdempotencyRecord> find(
                IdempotencyScope scope,
                String idempotencyKey
        ) {
            return Optional.ofNullable(record);
        }

        @Override
        public IdempotencyRecord reserve(
                IdempotencyScope scope,
                String idempotencyKey,
                String requestHash
        ) {
            Instant now = Instant.parse("2026-01-01T00:00:00Z");

            record = new IdempotencyRecord(
                    scope,
                    idempotencyKey,
                    requestHash,
                    IdempotencyStatus.PROCESSING,
                    null,
                    null,
                    now,
                    now
            );

            return record;
        }

        @Override
        public void markSucceeded(
                IdempotencyScope scope,
                String idempotencyKey,
                String responsePayload
        ) {
            Instant now = Instant.parse("2026-01-01T00:00:00Z");

            record = new IdempotencyRecord(
                    scope,
                    idempotencyKey,
                    record.requestHash(),
                    IdempotencyStatus.SUCCEEDED,
                    responsePayload,
                    null,
                    record.createdAt(),
                    now
            );
        }

        @Override
        public void markFailed(
                IdempotencyScope scope,
                String idempotencyKey,
                String failureCode
        ) {
            Instant now = Instant.parse("2026-01-01T00:00:00Z");

            record = new IdempotencyRecord(
                    scope,
                    idempotencyKey,
                    record.requestHash(),
                    IdempotencyStatus.FAILED,
                    null,
                    failureCode,
                    record.createdAt(),
                    now
            );
        }
    }

    private static final class FakeRequestHashPort implements RequestHashPort {

        @Override
        public String hash(CreatePaymentCommand command) {
            return "payment-request-hash";
        }

        @Override
        public String hash(RequestRefundCommand command) {
            return "refund-request-hash";
        }

        @Override
        public String hash(RequestPayoutCommand command) {
            return "payout-request-hash";
        }
    }

    private static final class FakeLedgerAccountLookupPort implements LedgerAccountLookupPort {

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
        public LedgerAccountId refundClearingAccount() {
            return new LedgerAccountId(UUID.randomUUID());
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
        public Map<ShopId, LedgerAccountId> sellerPendingAccountsFor(List<ShopId> shopIds) {
            return shopIds.stream()
                    .distinct()
                    .collect(Collectors.toMap(
                            shopId -> shopId,
                            shopId -> new LedgerAccountId(UUID.randomUUID())
                    ));
        }

        @Override
        public Map<PaymentAllocationId, LedgerAccountId> sellerRefundAccountsFor(
                List<PaymentAllocationId> paymentAllocationIds
        ) {
            return paymentAllocationIds.stream()
                    .distinct()
                    .collect(Collectors.toMap(
                            paymentAllocationId -> paymentAllocationId,
                            paymentAllocationId -> new LedgerAccountId(UUID.randomUUID())
                    ));
        }

        @Override
        public LedgerAccountId codClearingAccount() {
            return new LedgerAccountId(UUID.randomUUID());
        }
    }

    private static final class FakeLedgerPostingRepositoryPort
            implements LedgerPostingRepositoryPort {

        private final List<LedgerPosting> savedPostings = new ArrayList<>();

        @Override
        public void save(LedgerPosting posting) {
            savedPostings.add(posting);
        }
    }

    private static final class FakeOutboxPort implements OutboxPort {

        private final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void save(DomainEvent event) {
            events.add(event);
        }
    }

    private static final class ImmediateTransactionPort implements TransactionPort {

        @Override
        public <T> T execute(Supplier<T> action) {
            return action.get();
        }
    }

    private static final class FixedIdGeneratorPort implements IdGeneratorPort {

        private final PayoutId payoutId;

        private FixedIdGeneratorPort(PayoutId payoutId) {
            this.payoutId = payoutId;
        }

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
            return payoutId;
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

    private static final class PipeSeparatedRequestPayoutResultPayloadPort
            implements RequestPayoutResultPayloadPort {

        @Override
        public String serialize(RequestPayoutResult result) {
            return result.payoutId()
                    + "|"
                    + result.walletId()
                    + "|"
                    + result.shopId()
                    + "|"
                    + result.amount()
                    + "|"
                    + result.currency()
                    + "|"
                    + result.status();
        }

        @Override
        public RequestPayoutResult deserialize(String payload) {
            String[] parts = payload.split("\\|", -1);

            return new RequestPayoutResult(
                    UUID.fromString(parts[0]),
                    UUID.fromString(parts[1]),
                    UUID.fromString(parts[2]),
                    Long.parseLong(parts[3]),
                    parts[4],
                    parts[5]
            );
        }
    }
}