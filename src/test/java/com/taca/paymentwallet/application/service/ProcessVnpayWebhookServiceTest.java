package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.ProcessVnpayWebhookCommand;
import com.taca.paymentwallet.application.exception.PaymentAmountMismatchException;
import com.taca.paymentwallet.application.exception.PaymentAttemptNotFoundException;
import com.taca.paymentwallet.application.exception.PaymentNotFoundException;
import com.taca.paymentwallet.application.fee.PaymentFeePolicy;
import com.taca.paymentwallet.application.paymentevent.PaymentProviderEvent;
import com.taca.paymentwallet.application.port.out.*;
import com.taca.paymentwallet.application.result.ProcessVnpayWebhookResult;
import com.taca.paymentwallet.application.result.WebhookProcessingAction;
import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.finance.AllocationCalculator;
import com.taca.paymentwallet.domain.payment.*;
import com.taca.paymentwallet.domain.valueobject.*;
import com.taca.paymentwallet.domain.wallet.*;
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

        FakeWalletRepositoryPort walletRepository =
                new FakeWalletRepositoryPort();

        FakeOutboxPort outboxPort = new FakeOutboxPort();

        FakePaymentAttemptRepositoryPort
                paymentAttemptRepository =
                new FakePaymentAttemptRepositoryPort(
                        createPendingAttempt(
                                paymentId,
                                "vnpay-txn-001"
                        )
                );

        ProcessVnpayWebhookService service = newService(
                paymentRepository,
                paymentAttemptRepository,
                paymentProviderEventPort,
                allocationRepository,
                walletRepository,
                ledgerPostingRepository,
                outboxPort
        );

        ProcessVnpayWebhookResult result = service.execute(successCommand(100_000));

        assertEquals(paymentId.value(), result.paymentId());
        assertEquals("SUCCESS", result.paymentStatus());
        assertEquals(WebhookProcessingAction.APPLIED, result.action());

        assertEquals(PaymentStatus.SUCCESS, paymentRepository.payment.status());
        assertEquals(1, paymentRepository.savedPayments.size());
        assertEquals(1, allocationRepository.savedAllocations.size());
        assertEquals(1, ledgerPostingRepository.savedPostings.size());
        assertEquals("PAYMENT_CAPTURE", ledgerPostingRepository.savedPostings.getFirst().postingType());
        assertTrue(paymentProviderEventPort.applied);
        assertEquals(
                2,
                outboxPort.events.size()
        );

        assertTrue(
                outboxPort.events.stream()
                        .anyMatch(event ->
                                event instanceof PaymentSucceededEvent
                        )
        );

        assertTrue(
                outboxPort.events.stream()
                        .anyMatch(event ->
                                event instanceof WalletAllocatedEvent
                        )
        );
        assertEquals(
                PaymentAttemptStatus.SUCCESS,
                paymentAttemptRepository.attempt.status()
        );
        assertEquals(1, paymentAttemptRepository.savedAttempts.size());

        PaymentAllocation allocation =
                allocationRepository
                        .savedAllocations
                        .getFirst();

        WalletAllocatedEvent allocatedEvent =
                outboxPort.events
                        .stream()
                        .filter(
                                WalletAllocatedEvent.class::isInstance
                        )
                        .map(
                                WalletAllocatedEvent.class::cast
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                allocation.walletId(),
                allocatedEvent.walletId()
        );

        assertEquals(
                allocation.orderId(),
                allocatedEvent.orderId()
        );

        assertEquals(
                allocation.shopId(),
                allocatedEvent.shopId()
        );

        assertEquals(
                allocation.grossAmount(),
                allocatedEvent.grossAmount()
        );

        assertEquals(
                allocation.commissionAmount(),
                allocatedEvent.commissionAmount()
        );

        assertEquals(
                allocation.taxAmount(),
                allocatedEvent.taxAmount()
        );

        assertEquals(
                allocation.sellerNetAmount(),
                allocatedEvent.sellerNetAmount()
        );

        assertEquals(
                Instant.parse(
                        "2026-01-01T00:00:00Z"
                ),
                allocatedEvent.occurredAt()
        );
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

        FakeWalletRepositoryPort walletRepository =
                new FakeWalletRepositoryPort();

        FakeOutboxPort outboxPort = new FakeOutboxPort();

        FakePaymentAttemptRepositoryPort
                paymentAttemptRepository =
                new FakePaymentAttemptRepositoryPort(
                        createPendingAttempt(
                                paymentId,
                                "vnpay-txn-001"
                        )
                );

        ProcessVnpayWebhookService service = newService(
                paymentRepository,
                paymentAttemptRepository,
                paymentProviderEventPort,
                allocationRepository,
                walletRepository,
                ledgerPostingRepository,
                outboxPort
        );

        ProcessVnpayWebhookResult result = service.execute(successCommand(100_000));

        assertEquals(paymentId.value(), result.paymentId());
        assertEquals("PENDING", result.paymentStatus());
        assertEquals(WebhookProcessingAction.DUPLICATE, result.action());

        assertEquals(0, paymentRepository.savedPayments.size());
        assertEquals(0, allocationRepository.savedAllocations.size());
        assertEquals(0, ledgerPostingRepository.savedPostings.size());
        assertEquals(0, outboxPort.events.size());
        assertEquals(PaymentAttemptStatus.PENDING, paymentAttemptRepository.attempt.status());
        assertEquals(0, paymentAttemptRepository.savedAttempts.size());
    }

    @Test
    void shouldIgnoreNewWebhookWhenPaymentAlreadySucceeded() {
        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());

        ShopId shopId =
                new ShopId(UUID.randomUUID());

        Payment payment =
                createPendingVnpayPayment(
                        paymentId,
                        shopId
                );

        payment.markSucceeded(
                Instant.parse("2026-01-01T00:00:00Z")
        );

        payment.clearDomainEvents();

        FakePaymentRepositoryPort paymentRepository =
                new FakePaymentRepositoryPort(payment);

        FakePaymentProviderEventPort paymentProviderEventPort =
                new FakePaymentProviderEventPort(true);

        FakePaymentAllocationRepositoryPort allocationRepository =
                new FakePaymentAllocationRepositoryPort();

        FakeWalletRepositoryPort walletRepository =
                new FakeWalletRepositoryPort();

        FakeLedgerPostingRepositoryPort ledgerPostingRepository =
                new FakeLedgerPostingRepositoryPort();

        FakeOutboxPort outboxPort =
                new FakeOutboxPort();

        FakePaymentAttemptRepositoryPort
                paymentAttemptRepository =
                new FakePaymentAttemptRepositoryPort(
                        createPendingAttempt(
                                paymentId,
                                "vnpay-txn-001"
                        )
                );

        ProcessVnpayWebhookService service = newService(
                paymentRepository,
                paymentAttemptRepository,
                paymentProviderEventPort,
                allocationRepository,
                walletRepository,
                ledgerPostingRepository,
                outboxPort
        );

        ProcessVnpayWebhookResult result =
                service.execute(
                        successCommand(
                                100_000
                        )
                );

        assertEquals(paymentId.value(), result.paymentId());
        assertEquals("SUCCESS", result.paymentStatus());
        assertEquals(WebhookProcessingAction.DUPLICATE, result.action());
        assertEquals(0, paymentRepository.savedPayments.size());
        assertEquals(0, allocationRepository.savedAllocations.size());
        assertEquals(0, ledgerPostingRepository.savedPostings.size());
        assertEquals(0, outboxPort.events.size());
        assertTrue(paymentProviderEventPort.applied);
        assertEquals(0, paymentAttemptRepository.savedAttempts.size());
    }

    @Test
    void shouldIgnoreNewWebhookWhenPaymentAlreadyFailed() {
        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());

        ShopId shopId =
                new ShopId(UUID.randomUUID());

        Payment payment =
                createPendingVnpayPayment(
                        paymentId,
                        shopId
                );

        payment.markFailed(
                "VNPAY_24_02"
        );

        payment.clearDomainEvents();

        FakePaymentRepositoryPort paymentRepository =
                new FakePaymentRepositoryPort(payment);

        FakePaymentProviderEventPort paymentProviderEventPort =
                new FakePaymentProviderEventPort(true);

        FakePaymentAllocationRepositoryPort allocationRepository =
                new FakePaymentAllocationRepositoryPort();

        FakeWalletRepositoryPort walletRepository =
                new FakeWalletRepositoryPort();

        FakeLedgerPostingRepositoryPort ledgerPostingRepository =
                new FakeLedgerPostingRepositoryPort();

        FakeOutboxPort outboxPort =
                new FakeOutboxPort();

        FakePaymentAttemptRepositoryPort
                paymentAttemptRepository =
                new FakePaymentAttemptRepositoryPort(
                        createPendingAttempt(
                                paymentId,
                                "vnpay-txn-001"
                        )
                );

        ProcessVnpayWebhookService service =
                newService(
                        paymentRepository,
                        paymentAttemptRepository,
                        paymentProviderEventPort,
                        allocationRepository,
                        walletRepository,
                        ledgerPostingRepository,
                        outboxPort
                );

        ProcessVnpayWebhookResult result =
                service.execute(
                        successCommand(
                                100_000
                        )
                );

        assertEquals(paymentId.value(), result.paymentId());
        assertEquals("FAILED", result.paymentStatus());
        assertEquals(WebhookProcessingAction.DUPLICATE, result.action());
        assertEquals(PaymentStatus.FAILED, paymentRepository.payment.status());
        assertEquals("VNPAY_24_02", paymentRepository.payment.failureCode());
        assertEquals(0, paymentRepository.savedPayments.size());
        assertEquals(0, allocationRepository.savedAllocations.size());
        assertEquals(0, ledgerPostingRepository.savedPostings.size());
        assertEquals(0, outboxPort.events.size());
        assertTrue(paymentProviderEventPort.applied);
        assertEquals(0, paymentAttemptRepository.savedAttempts.size());
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

        FakeWalletRepositoryPort walletRepository =
                new FakeWalletRepositoryPort();

        FakePaymentAttemptRepositoryPort
                paymentAttemptRepository =
                new FakePaymentAttemptRepositoryPort(
                        createPendingAttempt(
                                paymentId,
                                "vnpay-txn-002"
                        )
                );

        ProcessVnpayWebhookService service = newService(
                paymentRepository,
                paymentAttemptRepository,
                paymentProviderEventPort,
                allocationRepository,
                walletRepository,
                ledgerPostingRepository,
                outboxPort
        );

        ProcessVnpayWebhookResult result = service.execute(failedCommand(100_000));

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
        assertEquals(
                PaymentAttemptStatus.FAILED,
                paymentAttemptRepository
                        .attempt
                        .status()
        );

        assertEquals(
                "VNPAY_24_02",
                paymentAttemptRepository
                        .attempt
                        .failureCode()
        );

        assertEquals(
                1,
                paymentAttemptRepository
                        .savedAttempts
                        .size()
        );

        assertEquals(
                1,
                outboxPort.events.size()
        );

        assertTrue(
                outboxPort.events.stream()
                        .anyMatch(event ->
                                event instanceof PaymentFailedEvent
                        )
        );

        assertTrue(
                outboxPort.events.stream()
                        .noneMatch(event ->
                                event instanceof WalletAllocatedEvent
                        )
        );
    }

    @Test
    void shouldRejectAmountMismatch() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        ShopId shopId = new ShopId(UUID.randomUUID());

        FakePaymentRepositoryPort paymentRepository =
                new FakePaymentRepositoryPort(createPendingVnpayPayment(paymentId, shopId));

        FakePaymentAttemptRepositoryPort
                paymentAttemptRepository =
                new FakePaymentAttemptRepositoryPort(
                        createPendingAttempt(
                                paymentId,
                                "vnpay-txn-001"
                        )
                );

        ProcessVnpayWebhookService service = newService(
                paymentRepository,
                paymentAttemptRepository,
                new FakePaymentProviderEventPort(true),
                new FakePaymentAllocationRepositoryPort(),
                new FakeWalletRepositoryPort(),
                new FakeLedgerPostingRepositoryPort(),
                new FakeOutboxPort()
        );

        assertThrows(
                PaymentAmountMismatchException.class,
                () -> service.execute(successCommand(90_000))
        );
    }

    @Test
    void shouldRejectUnknownPayment() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());

        FakePaymentAttemptRepositoryPort
                paymentAttemptRepository =
                new FakePaymentAttemptRepositoryPort(
                        createPendingAttempt(
                                paymentId,
                                "vnpay-txn-001"
                        )
                );

        ProcessVnpayWebhookService service = newService(
                new FakePaymentRepositoryPort(null),
                paymentAttemptRepository,
                new FakePaymentProviderEventPort(true),
                new FakePaymentAllocationRepositoryPort(),
                new FakeWalletRepositoryPort(),
                new FakeLedgerPostingRepositoryPort(),
                new FakeOutboxPort()
        );

        assertThrows(
                PaymentNotFoundException.class,
                () -> service.execute(successCommand(100_000))
        );
    }

    private ProcessVnpayWebhookService newService(
            FakePaymentRepositoryPort paymentRepository,
            FakePaymentAttemptRepositoryPort paymentAttemptRepository,
            FakePaymentProviderEventPort paymentProviderEventPort,
            FakePaymentAllocationRepositoryPort allocationRepository,
            FakeWalletRepositoryPort walletRepository,
            FakeLedgerPostingRepositoryPort ledgerPostingRepository,
            FakeOutboxPort outboxPort
    ) {
        return new ProcessVnpayWebhookService(
                paymentRepository,
                paymentAttemptRepository,
                paymentProviderEventPort,
                allocationRepository,
                walletRepository,
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

    @Test
    void shouldResolvePaymentFromProviderTransactionRef() {
        PaymentId actualPaymentId =
                new PaymentId(
                        UUID.randomUUID()
                );


        ShopId shopId =
                new ShopId(
                        UUID.randomUUID()
                );

        FakePaymentRepositoryPort paymentRepository =
                new FakePaymentRepositoryPort(
                        createPendingVnpayPayment(
                                actualPaymentId,
                                shopId
                        )
                );

        FakePaymentAttemptRepositoryPort
                paymentAttemptRepository =
                new FakePaymentAttemptRepositoryPort(
                        createPendingAttempt(
                                actualPaymentId,
                                "vnpay-txn-001"
                        )
                );

        ProcessVnpayWebhookService service =
                newService(
                        paymentRepository,
                        paymentAttemptRepository,
                        new FakePaymentProviderEventPort(
                                true
                        ),
                        new FakePaymentAllocationRepositoryPort(),
                        new FakeWalletRepositoryPort(),
                        new FakeLedgerPostingRepositoryPort(),
                        new FakeOutboxPort()
                );

        ProcessVnpayWebhookCommand command =
                new ProcessVnpayWebhookCommand(
                        "vnpay-event-001",
                        "vnpay-txn-001",
                        "00",
                        "00",
                        100_000,
                        "VND",
                        "payload-hash",
                        Map.of(
                                "vnp_SecureHash",
                                "signed-value"
                        )
                );

        ProcessVnpayWebhookResult result =
                service.execute(
                        command
                );

        assertEquals(
                actualPaymentId.value(),
                result.paymentId()
        );
    }

    @Test
    void shouldRejectUnknownPaymentAttempt() {
        ProcessVnpayWebhookService service =
                newService(
                        new FakePaymentRepositoryPort(
                                null
                        ),
                        new FakePaymentAttemptRepositoryPort(
                                null
                        ),
                        new FakePaymentProviderEventPort(
                                true
                        ),
                        new FakePaymentAllocationRepositoryPort(),
                        new FakeWalletRepositoryPort(),
                        new FakeLedgerPostingRepositoryPort(),
                        new FakeOutboxPort()
                );

        assertThrows(
                PaymentAttemptNotFoundException.class,
                () ->
                        service.execute(
                                successCommand(
                                        100_000
                                )
                        )
        );
    }

    @Test
    void shouldPostShippingFeeToShipmentPayableOnSuccessfulVnpayCapture() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());

        ShopId shopId = new ShopId(UUID.randomUUID());

        String providerTransactionRef = "vnpay-txn-shipping-001";

        Payment payment = Payment.create(
                paymentId,
                new CheckoutGroupId(UUID.randomUUID()),
                new BuyerUserId(UUID.randomUUID()),
                PaymentMethod.VNPAY,
                Money.vnd(120_000),
                List.of(
                        new PaymentOrder(
                                new OrderId(UUID.randomUUID()),
                                shopId,
                                Money.vnd(100_000),
                                Money.vnd(20_000)
                        )
                ),
                Instant.parse("2026-01-01T00:15:00Z")
        );

        payment.clearDomainEvents();

        FakePaymentRepositoryPort paymentRepository =
                new FakePaymentRepositoryPort(payment);

        FakePaymentProviderEventPort paymentProviderEventPort =
                new FakePaymentProviderEventPort(true);

        FakePaymentAllocationRepositoryPort allocationRepository =
                new FakePaymentAllocationRepositoryPort();

        FakeWalletRepositoryPort walletRepository =
                new FakeWalletRepositoryPort();

        FakeLedgerPostingRepositoryPort ledgerPostingRepository =
                new FakeLedgerPostingRepositoryPort();

        FakeOutboxPort outboxPort =
                new FakeOutboxPort();

        FakePaymentAttemptRepositoryPort paymentAttemptRepository =
                new FakePaymentAttemptRepositoryPort(
                        createPendingAttempt(
                                paymentId,
                                providerTransactionRef
                        )
                );

        ProcessVnpayWebhookService service = newService(
                paymentRepository,
                paymentAttemptRepository,
                paymentProviderEventPort,
                allocationRepository,
                walletRepository,
                ledgerPostingRepository,
                outboxPort
        );

        ProcessVnpayWebhookResult result = service.execute(
                successCommand(
                        120_000,
                        providerTransactionRef
                )
        );

        assertEquals("SUCCESS", result.paymentStatus());

        PaymentAllocation allocation =
                allocationRepository
                        .savedAllocations
                        .getFirst();

        assertEquals(
                Money.vnd(100_000),
                allocation.grossAmount()
        );

        LedgerPosting posting =
                ledgerPostingRepository
                        .savedPostings
                        .getFirst();

        long debit =
                posting.entries()
                        .stream()
                        .filter(
                                entry ->
                                        entry.entryType()
                                                == LedgerEntryType.DEBIT
                        )
                        .map(LedgerEntry::amount)
                        .mapToLong(Money::amount)
                        .sum();

        long credit =
                posting.entries()
                        .stream()
                        .filter(
                                entry ->
                                        entry.entryType()
                                                == LedgerEntryType.CREDIT
                        )
                        .map(LedgerEntry::amount)
                        .mapToLong(Money::amount)
                        .sum();

        assertEquals(120_000L, debit);

        assertEquals(120_000L, credit);
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

        private Wallet findByShopId(
                ShopId shopId
        ) {
            return walletsById.values()
                    .stream()
                    .filter(wallet ->
                            wallet.shopId()
                                    .equals(shopId)
                    )
                    .findFirst()
                    .orElseThrow();
        }
    }

    private Payment createPendingVnpayPayment(
            PaymentId paymentId,
            ShopId shopId
    ) {
        Payment payment =
                Payment.create(
                        paymentId,
                        new CheckoutGroupId(UUID.randomUUID()),
                        new BuyerUserId(UUID.randomUUID()),
                        PaymentMethod.VNPAY,
                        Money.vnd(100_000),
                        List.of(
                                new PaymentOrder(
                                        new OrderId(UUID.randomUUID()),
                                        shopId,
                                        Money.vnd(100_000)
                                )
                        ),
                        Instant.parse("2026-01-01T00:15:00Z")
                );

        payment.clearDomainEvents();

        return payment;
    }

    private ProcessVnpayWebhookCommand successCommand(
            long amount
    ) {
        return new ProcessVnpayWebhookCommand(
                "vnpay-event-001",
                "vnpay-txn-001",
                "00",
                "00",
                amount,
                "VND",
                "payload-hash-001",
                Map.of(
                        "vnp_SecureHash",
                        "signed-value"
                )
        );
    }

    private ProcessVnpayWebhookCommand successCommand(
            long amount,
            String providerTransactionRef
    ) {
        return new ProcessVnpayWebhookCommand(
                "vnpay-event-001",
                providerTransactionRef,
                "00",
                "00",
                amount,
                "VND",
                "payload-hash-001",
                Map.of(
                        "vnp_SecureHash",
                        "signed-value"
                )
        );
    }

    private ProcessVnpayWebhookCommand failedCommand(
            long amount
    ) {
        return new ProcessVnpayWebhookCommand(
                "vnpay-event-002",
                "vnpay-txn-002",
                "24",
                "02",
                amount,
                "VND",
                "payload-hash-002",
                Map.of(
                        "vnp_SecureHash",
                        "signed-value"
                )
        );
    }

    private PaymentAttempt createPendingAttempt(
            PaymentId paymentId,
            String providerTransactionRef
    ) {
        return PaymentAttempt.create(
                new PaymentAttemptId(UUID.randomUUID()),
                paymentId,
                "VNPAY",
                providerTransactionRef,
                "a".repeat(64),
                "b".repeat(64),
                Instant.parse("2026-01-01T00:15:00Z")
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
        public Optional<Payment> findByOrderId(OrderId orderId) {
            return Optional.empty();
        }

        @Override
        public Payment save(Payment payment) {
            this.payment = payment;
            this.savedPayments.add(payment);
            return payment;
        }
    }

    private static final class FakePaymentAttemptRepositoryPort implements PaymentAttemptRepositoryPort {

        private PaymentAttempt attempt;

        private final List<PaymentAttempt>
                savedAttempts =
                new ArrayList<>();

        private FakePaymentAttemptRepositoryPort(
                PaymentAttempt attempt
        ) {
            this.attempt = attempt;
        }

        @Override
        public PaymentAttempt save(
                PaymentAttempt attempt
        ) {
            this.attempt = attempt;

            savedAttempts.add(
                    attempt
            );

            return attempt;
        }

        @Override
        public Optional<PaymentAttempt>
        findByProviderAndProviderTransactionRef(
                String provider,
                String providerTransactionRef
        ) {
            if (attempt == null) {
                return Optional.empty();
            }

            if (!attempt.provider()
                    .equals(provider)) {
                return Optional.empty();
            }

            if (!attempt
                    .providerTransactionRef()
                    .equals(providerTransactionRef)) {
                return Optional.empty();
            }

            return Optional.of(attempt);
        }

        @Override
        public List<PaymentAttempt>
        findByPaymentId(
                PaymentId paymentId
        ) {
            if (attempt == null
                    || !attempt.paymentId().equals(paymentId)) {
                return List.of();
            }

            return List.of(attempt);
        }

        @Override
        public boolean existsPendingByPaymentId(
                PaymentId paymentId
        ) {
            return attempt != null
                    && attempt.paymentId()
                    .equals(paymentId)
                    && attempt.status()
                    == PaymentAttemptStatus.PENDING;
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
        public LedgerAccountId shipmentPayableAccount() {
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

        @Override
        public PaymentAttemptId nextPaymentAttemptId() {
            return new PaymentAttemptId(
                    UUID.randomUUID()
            );
        }
    }
}
