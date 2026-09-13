package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.ProcessCodPaymentCommand;
import com.taca.paymentwallet.application.exception.PaymentAmountMismatchException;
import com.taca.paymentwallet.application.exception.PaymentNotFoundByCheckoutGroupException;
import com.taca.paymentwallet.application.exception.UnsupportedPaymentMethodException;
import com.taca.paymentwallet.application.fee.PaymentFeePolicy;
import com.taca.paymentwallet.application.payment.CodPaymentProcessingAction;
import com.taca.paymentwallet.application.payment.CodPaymentResultStatus;
import com.taca.paymentwallet.application.port.out.*;
import com.taca.paymentwallet.application.result.ProcessCodPaymentResult;
import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.finance.AllocationCalculator;
import com.taca.paymentwallet.domain.payment.*;
import com.taca.paymentwallet.domain.valueobject.*;
import com.taca.paymentwallet.domain.wallet.LedgerPosting;
import com.taca.paymentwallet.domain.wallet.LedgerPostingFactory;
import com.taca.paymentwallet.domain.wallet.Wallet;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

class ProcessCodPaymentServiceTest {

    private final FakePaymentRepositoryPort paymentRepository = new FakePaymentRepositoryPort();
    private final FakePaymentAllocationRepositoryPort paymentAllocationRepository = new FakePaymentAllocationRepositoryPort();
    private final FakeWalletRepositoryPort walletRepository = new FakeWalletRepositoryPort();
    private final FakeLedgerPostingRepositoryPort ledgerPostingRepository = new FakeLedgerPostingRepositoryPort();
    private final FakeLedgerAccountLookupPort ledgerAccountLookupPort = new FakeLedgerAccountLookupPort();
    private final FakeFeePolicyPort feePolicyPort = new FakeFeePolicyPort();
    private final FakeIdGeneratorPort idGeneratorPort = new FakeIdGeneratorPort();
    private final FakeOutboxPort outboxPort = new FakeOutboxPort();
    private final FakeTransactionPort transactionPort = new FakeTransactionPort();

    private final ProcessCodPaymentService service = new ProcessCodPaymentService(
            paymentRepository,
            paymentAllocationRepository,
            walletRepository,
            ledgerPostingRepository,
            ledgerAccountLookupPort,
            feePolicyPort,
            idGeneratorPort,
            outboxPort,
            transactionPort,
            new AllocationCalculator(),
            new LedgerPostingFactory()
    );

    @Test
    void shouldApplyDeliveredCodPayment() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        Wallet wallet = Wallet.create(new WalletId(UUID.randomUUID()), shopId);
        Payment payment = codPayment(shopId, Money.vnd(100_000));

        paymentRepository.add(payment);
        walletRepository.add(wallet);

        ProcessCodPaymentResult result = service.execute(new ProcessCodPaymentCommand(
                payment.checkoutGroupId().value(),
                CodPaymentResultStatus.DELIVERED,
                100_000,
                "VND",
                Instant.parse("2026-09-12T01:00:00Z"),
                null
        ));

        assertThat(result.action()).isEqualTo(CodPaymentProcessingAction.APPLIED);
        assertThat(result.paymentStatus()).isEqualTo("SUCCESS");

        assertThat(payment.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.capturedAmount()).isEqualTo(Money.vnd(100_000));

        assertThat(paymentAllocationRepository.allocations).hasSize(1);
        assertThat(paymentAllocationRepository.allocations.getFirst().sellerNetAmount())
                .isEqualTo(Money.vnd(92_000));

        assertThat(wallet.pendingBalance()).isEqualTo(Money.vnd(92_000));
        assertThat(wallet.availableBalance()).isEqualTo(Money.vnd(0));

        assertThat(ledgerPostingRepository.postings).hasSize(1);
        assertThat(outboxPort.events).hasSize(1);
    }

    @Test
    void shouldApplyFailedCodPayment() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        Payment payment = codPayment(shopId, Money.vnd(100_000));
        paymentRepository.add(payment);

        ProcessCodPaymentResult result = service.execute(new ProcessCodPaymentCommand(
                payment.checkoutGroupId().value(),
                CodPaymentResultStatus.FAILED,
                100_000,
                "VND",
                Instant.parse("2026-09-12T01:00:00Z"),
                "SHIPMENT_FAILED"
        ));

        assertThat(result.action()).isEqualTo(CodPaymentProcessingAction.APPLIED);
        assertThat(result.paymentStatus()).isEqualTo("FAILED");

        assertThat(payment.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.failureCode()).isEqualTo("SHIPMENT_FAILED");

        assertThat(paymentAllocationRepository.allocations).isEmpty();
        assertThat(ledgerPostingRepository.postings).isEmpty();
        assertThat(outboxPort.events).hasSize(1);
    }

    @Test
    void shouldApplyCancelledCodPayment() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        Payment payment = codPayment(shopId, Money.vnd(100_000));
        paymentRepository.add(payment);

        ProcessCodPaymentResult result = service.execute(new ProcessCodPaymentCommand(
                payment.checkoutGroupId().value(),
                CodPaymentResultStatus.CANCELLED,
                100_000,
                "VND",
                Instant.parse("2026-09-12T01:00:00Z"),
                "ORDER_CANCELLED"
        ));

        assertThat(result.action()).isEqualTo(CodPaymentProcessingAction.APPLIED);
        assertThat(result.paymentStatus()).isEqualTo("FAILED");
        assertThat(payment.failureCode()).isEqualTo("ORDER_CANCELLED");
        assertThat(outboxPort.events).hasSize(1);
    }

    @Test
    void shouldIgnoreDuplicateTerminalCodPayment() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        Payment payment = codPayment(shopId, Money.vnd(100_000));
        payment.markSucceeded(Instant.parse("2026-09-12T01:00:00Z"));
        payment.clearDomainEvents();

        paymentRepository.add(payment);

        ProcessCodPaymentResult result = service.execute(new ProcessCodPaymentCommand(
                payment.checkoutGroupId().value(),
                CodPaymentResultStatus.DELIVERED,
                100_000,
                "VND",
                Instant.parse("2026-09-12T01:00:00Z"),
                null
        ));

        assertThat(result.action()).isEqualTo(CodPaymentProcessingAction.DUPLICATE);
        assertThat(result.paymentStatus()).isEqualTo("SUCCESS");
        assertThat(paymentAllocationRepository.allocations).isEmpty();
        assertThat(ledgerPostingRepository.postings).isEmpty();
        assertThat(outboxPort.events).isEmpty();
    }

    @Test
    void shouldRejectUnknownCheckoutGroup() {
        assertThatThrownBy(() -> service.execute(new ProcessCodPaymentCommand(
                UUID.randomUUID(),
                CodPaymentResultStatus.DELIVERED,
                100_000,
                "VND",
                Instant.parse("2026-09-12T01:00:00Z"),
                null
        ))).isInstanceOf(PaymentNotFoundByCheckoutGroupException.class);
    }

    @Test
    void shouldRejectAmountMismatch() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        Payment payment = codPayment(shopId, Money.vnd(100_000));
        paymentRepository.add(payment);

        assertThatThrownBy(() -> service.execute(new ProcessCodPaymentCommand(
                payment.checkoutGroupId().value(),
                CodPaymentResultStatus.DELIVERED,
                90_000,
                "VND",
                Instant.parse("2026-09-12T01:00:00Z"),
                null
        ))).isInstanceOf(PaymentAmountMismatchException.class);
    }

    @Test
    void shouldRejectNonCodPayment() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        Payment payment = vnpayPayment(shopId, Money.vnd(100_000));
        paymentRepository.add(payment);

        assertThatThrownBy(() -> service.execute(new ProcessCodPaymentCommand(
                payment.checkoutGroupId().value(),
                CodPaymentResultStatus.DELIVERED,
                100_000,
                "VND",
                Instant.parse("2026-09-12T01:00:00Z"),
                null
        ))).isInstanceOf(UnsupportedPaymentMethodException.class);
    }

    private Payment codPayment(ShopId shopId, Money amount) {
        return Payment.create(
                new PaymentId(UUID.randomUUID()),
                new CheckoutGroupId(UUID.randomUUID()),
                new BuyerUserId(UUID.randomUUID()),
                PaymentMethod.COD,
                amount,
                List.of(new PaymentOrder(
                        new OrderId(UUID.randomUUID()),
                        shopId,
                        amount
                ))
        );
    }

    private Payment vnpayPayment(ShopId shopId, Money amount) {
        return Payment.create(
                new PaymentId(UUID.randomUUID()),
                new CheckoutGroupId(UUID.randomUUID()),
                new BuyerUserId(UUID.randomUUID()),
                PaymentMethod.VNPAY,
                amount,
                List.of(new PaymentOrder(
                        new OrderId(UUID.randomUUID()),
                        shopId,
                        amount
                ))
        );
    }

    private static class FakePaymentRepositoryPort implements PaymentRepositoryPort {

        private final Map<PaymentId, Payment> paymentsById = new HashMap<>();
        private final Map<CheckoutGroupId, Payment> paymentsByCheckoutGroupId = new HashMap<>();

        void add(Payment payment) {
            paymentsById.put(payment.id(), payment);
            paymentsByCheckoutGroupId.put(payment.checkoutGroupId(), payment);
        }

        @Override
        public Optional<Payment> findById(PaymentId paymentId) {
            return Optional.ofNullable(paymentsById.get(paymentId));
        }

        @Override
        public Optional<Payment> findByCheckoutGroupId(CheckoutGroupId checkoutGroupId) {
            return Optional.ofNullable(paymentsByCheckoutGroupId.get(checkoutGroupId));
        }

        @Override
        public Optional<Payment> findByCheckoutGroupIdForUpdate(CheckoutGroupId checkoutGroupId) {
            return findByCheckoutGroupId(checkoutGroupId);
        }

        @Override
        public Payment save(Payment payment) {
            add(payment);
            return payment;
        }
    }

    private static class FakePaymentAllocationRepositoryPort implements PaymentAllocationRepositoryPort {

        private final List<PaymentAllocation> allocations = new ArrayList<>();

        @Override
        public void saveAll(List<PaymentAllocation> allocations) {
            this.allocations.addAll(allocations);
        }

        @Override
        public List<PaymentAllocation> findByPaymentId(PaymentId paymentId) {
            return allocations;
        }
    }

    private static class FakeWalletRepositoryPort implements WalletRepositoryPort {

        private final Map<WalletId, Wallet> walletsById = new HashMap<>();

        void add(Wallet wallet) {
            walletsById.put(wallet.id(), wallet);
        }

        @Override
        public Optional<Wallet> findById(WalletId walletId) {
            return Optional.ofNullable(walletsById.get(walletId));
        }

        @Override
        public Optional<Wallet> findByIdForUpdate(WalletId walletId) {
            return Optional.ofNullable(walletsById.get(walletId));
        }

        @Override
        public Optional<Wallet> findByShopIdAndCurrencyForUpdate(
                ShopId shopId,
                String currency
        ) {
            return walletsById.values().stream()
                    .filter(wallet -> wallet.shopId().equals(shopId))
                    .filter(wallet -> wallet.currency().equals(currency))
                    .findFirst();
        }

        @Override
        public Wallet save(Wallet wallet) {
            walletsById.put(wallet.id(), wallet);
            return wallet;
        }
    }

    private static class FakeLedgerPostingRepositoryPort implements LedgerPostingRepositoryPort {

        private final List<LedgerPosting> postings = new ArrayList<>();

        @Override
        public void save(LedgerPosting posting) {
            postings.add(posting);
        }
    }

    private static class FakeLedgerAccountLookupPort implements LedgerAccountLookupPort {

        @Override
        public LedgerAccountId vnpayClearingAccount() {
            return new LedgerAccountId(UUID.randomUUID());
        }

        @Override
        public LedgerAccountId codClearingAccount() {
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
            return Map.of();
        }

        @Override
        public LedgerAccountId sellerAvailableAccount(ShopId shopId) {
            return new LedgerAccountId(UUID.randomUUID());
        }

        @Override
        public LedgerAccountId payoutClearingAccount() {
            return new LedgerAccountId(UUID.randomUUID());
        }
    }

    private static class FakeFeePolicyPort implements FeePolicyPort {

        @Override
        public PaymentFeePolicy currentPaymentFeePolicy() {
            return new PaymentFeePolicy(
                    new RateBps(700),
                    new RateBps(100)
            );
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