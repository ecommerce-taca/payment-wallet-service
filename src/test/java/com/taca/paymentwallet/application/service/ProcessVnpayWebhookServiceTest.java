package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.ProcessVnpayWebhookCommand;
import com.taca.paymentwallet.application.exception.PaymentAmountMismatchException;
import com.taca.paymentwallet.application.exception.PaymentNotFoundException;
import com.taca.paymentwallet.application.fee.PaymentFeePolicy;
import com.taca.paymentwallet.application.paymentevent.PaymentProviderEvent;
import com.taca.paymentwallet.application.port.out.*;
import com.taca.paymentwallet.application.result.ProcessVnpayWebhookResult;
import com.taca.paymentwallet.application.result.WebhookProcessingAction;
import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.finance.AllocationCalculator;
import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.payment.PaymentMethod;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.payment.PaymentStatus;
import com.taca.paymentwallet.domain.valueobject.*;
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

class ProcessVnpayWebhookServiceTest {

    @Test
    void shouldApplySuccessfulVnpayWebhook() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        ShopId shopId = new ShopId(UUID.randomUUID());

        FakePaymentRepositoryPort paymentRepository =
                new FakePaymentRepositoryPort(createPendingVnpayPayment(paymentId, shopId));

        FakePaymentProviderEventPort paymentProviderEventPort =
                new FakePaymentProviderEventPort(true);

        FakePaymentAllocationRepositoryPort allocationRepository =
                new FakePaymentAllocationRepositoryPort();

        FakeLedgerPostingRepositoryPort ledgerPostingRepository =
                new FakeLedgerPostingRepositoryPort();

        FakeOutboxPort outboxPort = new FakeOutboxPort();

        ProcessVnpayWebhookService service = newService(
                paymentRepository,
                paymentProviderEventPort,
                allocationRepository,
                ledgerPostingRepository,
                outboxPort
        );

        ProcessVnpayWebhookResult result = service.execute(successCommand(paymentId.value(), 100_000));

        assertEquals(paymentId.value(), result.paymentId());
        assertEquals("SUCCESS", result.paymentStatus());
        assertEquals(WebhookProcessingAction.APPLIED, result.action());

        assertEquals(PaymentStatus.SUCCESS, paymentRepository.payment.status());
        assertEquals(1, paymentRepository.savedPayments.size());
        assertEquals(1, allocationRepository.savedAllocations.size());
        assertEquals(1, ledgerPostingRepository.savedPostings.size());
        assertEquals("PAYMENT_CAPTURE", ledgerPostingRepository.savedPostings.getFirst().postingType());
        assertTrue(paymentProviderEventPort.applied);
        assertTrue(outboxPort.events.stream()
                .anyMatch(event -> event.eventType().equals("payment.succeeded")));
    }

    @Test
    void shouldIgnoreDuplicateWebhook() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        ShopId shopId = new ShopId(UUID.randomUUID());

        FakePaymentRepositoryPort paymentRepository =
                new FakePaymentRepositoryPort(createPendingVnpayPayment(paymentId, shopId));

        FakePaymentProviderEventPort paymentProviderEventPort =
                new FakePaymentProviderEventPort(false);

        FakePaymentAllocationRepositoryPort allocationRepository =
                new FakePaymentAllocationRepositoryPort();

        FakeLedgerPostingRepositoryPort ledgerPostingRepository =
                new FakeLedgerPostingRepositoryPort();

        FakeOutboxPort outboxPort = new FakeOutboxPort();

        ProcessVnpayWebhookService service = newService(
                paymentRepository,
                paymentProviderEventPort,
                allocationRepository,
                ledgerPostingRepository,
                outboxPort
        );

        ProcessVnpayWebhookResult result = service.execute(successCommand(paymentId.value(), 100_000));

        assertEquals(paymentId.value(), result.paymentId());
        assertEquals("PENDING", result.paymentStatus());
        assertEquals(WebhookProcessingAction.DUPLICATE, result.action());

        assertEquals(0, paymentRepository.savedPayments.size());
        assertEquals(0, allocationRepository.savedAllocations.size());
        assertEquals(0, ledgerPostingRepository.savedPostings.size());
        assertEquals(0, outboxPort.events.size());
    }

    @Test
    void shouldMarkPaymentFailedWhenVnpayFailed() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        ShopId shopId = new ShopId(UUID.randomUUID());

        FakePaymentRepositoryPort paymentRepository =
                new FakePaymentRepositoryPort(createPendingVnpayPayment(paymentId, shopId));

        FakePaymentProviderEventPort paymentProviderEventPort =
                new FakePaymentProviderEventPort(true);

        FakePaymentAllocationRepositoryPort allocationRepository =
                new FakePaymentAllocationRepositoryPort();

        FakeLedgerPostingRepositoryPort ledgerPostingRepository =
                new FakeLedgerPostingRepositoryPort();

        FakeOutboxPort outboxPort = new FakeOutboxPort();

        ProcessVnpayWebhookService service = newService(
                paymentRepository,
                paymentProviderEventPort,
                allocationRepository,
                ledgerPostingRepository,
                outboxPort
        );

        ProcessVnpayWebhookResult result = service.execute(failedCommand(paymentId.value(), 100_000));

        assertEquals(paymentId.value(), result.paymentId());
        assertEquals("FAILED", result.paymentStatus());
        assertEquals(WebhookProcessingAction.APPLIED, result.action());

        assertEquals(PaymentStatus.FAILED, paymentRepository.payment.status());
        assertEquals("VNPAY_24_02", paymentRepository.payment.failureCode());

        assertEquals(1, paymentRepository.savedPayments.size());
        assertEquals(0, allocationRepository.savedAllocations.size());
        assertEquals(0, ledgerPostingRepository.savedPostings.size());
        assertTrue(paymentProviderEventPort.applied);
        assertTrue(outboxPort.events.stream()
                .anyMatch(event -> event.eventType().equals("payment.failed")));
    }

    @Test
    void shouldRejectAmountMismatch() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        ShopId shopId = new ShopId(UUID.randomUUID());

        FakePaymentRepositoryPort paymentRepository =
                new FakePaymentRepositoryPort(createPendingVnpayPayment(paymentId, shopId));

        ProcessVnpayWebhookService service = newService(
                paymentRepository,
                new FakePaymentProviderEventPort(true),
                new FakePaymentAllocationRepositoryPort(),
                new FakeLedgerPostingRepositoryPort(),
                new FakeOutboxPort()
        );

        assertThrows(
                PaymentAmountMismatchException.class,
                () -> service.execute(successCommand(paymentId.value(), 90_000))
        );
    }

    @Test
    void shouldRejectUnknownPayment() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());

        ProcessVnpayWebhookService service = newService(
                new FakePaymentRepositoryPort(null),
                new FakePaymentProviderEventPort(true),
                new FakePaymentAllocationRepositoryPort(),
                new FakeLedgerPostingRepositoryPort(),
                new FakeOutboxPort()
        );

        assertThrows(
                PaymentNotFoundException.class,
                () -> service.execute(successCommand(paymentId.value(), 100_000))
        );
    }

    private ProcessVnpayWebhookService newService(
            FakePaymentRepositoryPort paymentRepository,
            FakePaymentProviderEventPort paymentProviderEventPort,
            FakePaymentAllocationRepositoryPort allocationRepository,
            FakeLedgerPostingRepositoryPort ledgerPostingRepository,
            FakeOutboxPort outboxPort
    ) {
        return new ProcessVnpayWebhookService(
                paymentRepository,
                paymentProviderEventPort,
                allocationRepository,
                new FakeWalletRepositoryPort(),
                ledgerPostingRepository,
                new FakeLedgerAccountLookupPort(),
                new FixedFeePolicyPort(),
                new FakeVnpayWebhookVerifierPort(),
                new FakeIdGeneratorPort(),
                new FixedClockPort(),
                outboxPort,
                new ImmediateTransactionPort(),
                new AllocationCalculator(),
                new LedgerPostingFactory()
        );
    }

    private static final class FakeWalletRepositoryPort
            implements WalletRepositoryPort {

        private final Map<WalletId, Wallet> walletsById = new HashMap<>();

        @Override
        public Optional<Wallet> findById(WalletId walletId) {
            return Optional.ofNullable(
                    walletsById.get(walletId)
            );
        }

        @Override
        public Optional<Wallet> findByIdForUpdate(
                WalletId walletId
        ) {
            return findById(walletId);
        }

        @Override
        public Optional<Wallet> findByShopIdAndCurrencyForUpdate(
                ShopId shopId,
                String currency
        ) {
            Wallet existing = walletsById.values()
                    .stream()
                    .filter(wallet ->
                            wallet.shopId().equals(shopId))
                    .filter(wallet ->
                            wallet.currency().equals(currency))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                return Optional.of(existing);
            }

            Wallet wallet = Wallet.create(
                    new WalletId(UUID.randomUUID()),
                    shopId
            );

            walletsById.put(wallet.id(), wallet);

            return Optional.of(wallet);
        }

        @Override
        public Wallet save(Wallet wallet) {
            walletsById.put(wallet.id(), wallet);
            return wallet;
        }
    }

    private Payment createPendingVnpayPayment(PaymentId paymentId, ShopId shopId) {
        return Payment.create(
                paymentId,
                new CheckoutGroupId(UUID.randomUUID()),
                new BuyerUserId(UUID.randomUUID()),
                PaymentMethod.VNPAY,
                Money.vnd(100_000),
                List.of(new PaymentOrder(
                        new OrderId(UUID.randomUUID()),
                        shopId,
                        Money.vnd(100_000)
                )),
                Instant.parse("2026-01-01T00:15:00Z")
        );
    }

    private ProcessVnpayWebhookCommand successCommand(UUID paymentId, long amount) {
        return new ProcessVnpayWebhookCommand(
                paymentId,
                "vnpay-event-001",
                "vnpay-txn-001",
                "00",
                "00",
                amount,
                "VND",
                "payload-hash-001",
                Map.of("vnp_SecureHash", "signed-value")
        );
    }

    private ProcessVnpayWebhookCommand failedCommand(UUID paymentId, long amount) {
        return new ProcessVnpayWebhookCommand(
                paymentId,
                "vnpay-event-002",
                "vnpay-txn-002",
                "24",
                "02",
                amount,
                "VND",
                "payload-hash-002",
                Map.of("vnp_SecureHash", "signed-value")
        );
    }

    private static final class FakePaymentRepositoryPort implements PaymentRepositoryPort {

        private final List<Payment> savedPayments = new ArrayList<>();
        private Payment payment;

        private FakePaymentRepositoryPort(Payment payment) {
            this.payment = payment;
        }

        @Override
        public Optional<Payment> findById(PaymentId paymentId) {
            return Optional.ofNullable(payment);
        }

        @Override
        public Optional<Payment> findByIdForUpdate(PaymentId paymentId) {
            return Optional.ofNullable(payment);
        }

        @Override
        public Optional<Payment> findByCheckoutGroupId(CheckoutGroupId checkoutGroupId) {
            return Optional.empty();
        }

        @Override
        public Payment save(Payment payment) {
            this.payment = payment;
            this.savedPayments.add(payment);
            return payment;
        }
    }

    private static final class FakePaymentProviderEventPort implements PaymentProviderEventPort {

        private final boolean insertResult;
        private boolean applied;

        private FakePaymentProviderEventPort(boolean insertResult) {
            this.insertResult = insertResult;
        }

        @Override
        public boolean recordIfAbsent(PaymentProviderEvent event) {
            return insertResult;
        }

        @Override
        public void markApplied(String provider, String providerEventId) {
            this.applied = true;
        }
    }

    private static final class FakePaymentAllocationRepositoryPort
            implements PaymentAllocationRepositoryPort {

        private final List<PaymentAllocation> savedAllocations = new ArrayList<>();

        @Override
        public void saveAll(List<PaymentAllocation> allocations) {
            savedAllocations.addAll(allocations);
        }

        @Override
        public List<PaymentAllocation> findByPaymentId(PaymentId paymentId) {
            return List.of();
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

        @Override
        public Map<ShopId, LedgerAccountId> sellerPendingAccountsFor(List<ShopId> shopIds) {
            return shopIds.stream()
                    .distinct()
                    .collect(
                            java.util.stream.Collectors.toMap(
                                    shopId -> shopId,
                                    shopId -> new LedgerAccountId(UUID.randomUUID())
                            )
                    );
        }

        @Override
        public LedgerAccountId refundClearingAccount() {
            return new LedgerAccountId(UUID.randomUUID());
        }

        @Override
        public Map<PaymentAllocationId, LedgerAccountId> sellerRefundAccountsFor(
                List<PaymentAllocationId> paymentAllocationIds
        ) {
            return paymentAllocationIds.stream()
                    .collect(Collectors.toMap(
                            id -> id,
                            id -> new LedgerAccountId(UUID.randomUUID())
                    ));
        }
    }

    private static final class FixedFeePolicyPort implements FeePolicyPort {

        @Override
        public PaymentFeePolicy currentPaymentFeePolicy() {
            return new PaymentFeePolicy(
                    new FeeConfigId(
                            UUID.fromString(
                                    "11111111-1111-1111-1111-111111111111"
                            )
                    ),
                    new TaxConfigId(
                            UUID.fromString(
                                    "22222222-2222-2222-2222-222222222222"
                            )
                    ),
                    RateBps.of(700),
                    RateBps.of(100)
            );
        }
    }

    private static final class FakeVnpayWebhookVerifierPort implements VnpayWebhookVerifierPort {

        @Override
        public void verify(ProcessVnpayWebhookCommand command) {
        }
    }

    private static final class FixedClockPort implements ClockPort {

        @Override
        public Instant now() {
            return Instant.parse("2026-01-01T00:00:00Z");
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

    private static final class FakeIdGeneratorPort implements IdGeneratorPort {

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
