package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.ProcessCodPaymentCommand;
import com.taca.paymentwallet.application.exception.PaymentNotFoundByOrderException;
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
import com.taca.paymentwallet.domain.wallet.WalletAllocatedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessCodPaymentServiceTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-10-02T10:00:00Z");

    private final FakePaymentRepositoryPort paymentRepository = new FakePaymentRepositoryPort();
    private final FakePaymentAllocationRepositoryPort allocationRepository = new FakePaymentAllocationRepositoryPort();
    private final FakeWalletRepositoryPort walletRepository = new FakeWalletRepositoryPort();
    private final FakeLedgerPostingRepositoryPort ledgerRepository = new FakeLedgerPostingRepositoryPort();
    private final FakeLedgerAccountLookupPort ledgerAccountLookup = new FakeLedgerAccountLookupPort();
    private final FakeFeePolicyPort feePolicy = new FakeFeePolicyPort();
    private final FakeIdGeneratorPort idGenerator = new FakeIdGeneratorPort();
    private final FakeOutboxPort outbox = new FakeOutboxPort();
    private final FakeTransactionPort transactionPort = new FakeTransactionPort();

    private final ProcessCodPaymentService service = new ProcessCodPaymentService(
            paymentRepository,
            allocationRepository,
            walletRepository,
            ledgerRepository,
            ledgerAccountLookup,
            feePolicy,
            idGenerator,
            outbox,
            transactionPort,
            new AllocationCalculator(),
            new LedgerPostingFactory()
    );

    @Test
    void shouldApplyDeliveredSingleOrderCodPayment() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        Payment payment = codPayment(shopId, Money.vnd(100_000));
        Wallet wallet = Wallet.create(new WalletId(UUID.randomUUID()), shopId);

        paymentRepository.add(payment);
        walletRepository.add(wallet);

        UUID orderId = payment.orders().getFirst().orderId().value();

        ProcessCodPaymentResult result = service.execute(new ProcessCodPaymentCommand(
                orderId,
                CodPaymentResultStatus.DELIVERED,
                OCCURRED_AT,
                null
        ));

        assertThat(result.action()).isEqualTo(CodPaymentProcessingAction.APPLIED);
        assertThat(result.paymentId()).isEqualTo(payment.id().value());
        assertThat(result.checkoutGroupId()).isEqualTo(payment.checkoutGroupId().value());
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.paymentStatus()).isEqualTo("SUCCESS");
        assertThat(result.orderCodStatus()).isEqualTo("CAPTURED");

        assertThat(payment.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.capturedAmount()).isEqualTo(Money.vnd(100_000));
        assertThat(payment.order(new OrderId(orderId)).codStatus())
                .isEqualTo(PaymentOrderCodStatus.CAPTURED);

        assertThat(allocationRepository.allocations).hasSize(1);

        PaymentAllocation allocation = allocationRepository.allocations.getFirst();

        assertThat(allocation.orderId()).isEqualTo(new OrderId(orderId));
        assertThat(allocation.sellerNetAmount()).isEqualTo(Money.vnd(92_000));
        assertThat(wallet.pendingBalance()).isEqualTo(Money.vnd(92_000));
        assertThat(wallet.availableBalance()).isEqualTo(Money.vnd(0));

        assertThat(ledgerRepository.postings).hasSize(1);

        LedgerPosting posting = ledgerRepository.postings.getFirst();

        assertThat(posting.postingType()).isEqualTo("COD_CAPTURE");
        assertThat(posting.businessKey())
                .isEqualTo("COD_CAPTURE:" + payment.id().value() + ":" + orderId);

        assertThat(outbox.events).hasSize(2);
        assertThat(outbox.events).anyMatch(PaymentSucceededEvent.class::isInstance);
        assertThat(outbox.events).anyMatch(WalletAllocatedEvent.class::isInstance);

        WalletAllocatedEvent allocatedEvent = outbox.events.stream()
                .filter(WalletAllocatedEvent.class::isInstance)
                .map(WalletAllocatedEvent.class::cast)
                .findFirst()
                .orElseThrow();

        assertThat(allocatedEvent.walletId()).isEqualTo(allocation.walletId());
        assertThat(allocatedEvent.orderId()).isEqualTo(allocation.orderId());
        assertThat(allocatedEvent.shopId()).isEqualTo(allocation.shopId());
        assertThat(allocatedEvent.grossAmount()).isEqualTo(allocation.grossAmount());
        assertThat(allocatedEvent.commissionAmount()).isEqualTo(allocation.commissionAmount());
        assertThat(allocatedEvent.taxAmount()).isEqualTo(allocation.taxAmount());
        assertThat(allocatedEvent.sellerNetAmount()).isEqualTo(allocation.sellerNetAmount());
        assertThat(allocatedEvent.occurredAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void shouldApplyFailedSingleOrderCodPayment() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        Payment payment = codPayment(shopId, Money.vnd(100_000));

        paymentRepository.add(payment);

        UUID orderId = payment.orders().getFirst().orderId().value();

        ProcessCodPaymentResult result = service.execute(new ProcessCodPaymentCommand(
                orderId,
                CodPaymentResultStatus.FAILED,
                OCCURRED_AT,
                "SHIPMENT_FAILED"
        ));

        assertThat(result.action()).isEqualTo(CodPaymentProcessingAction.APPLIED);
        assertThat(result.paymentStatus()).isEqualTo("FAILED");
        assertThat(result.orderCodStatus()).isEqualTo("FAILED");

        PaymentOrder order = payment.order(new OrderId(orderId));

        assertThat(order.codStatus()).isEqualTo(PaymentOrderCodStatus.FAILED);
        assertThat(order.codProcessedAt()).isEqualTo(OCCURRED_AT);
        assertThat(order.codFailureCode()).isEqualTo("SHIPMENT_FAILED");

        assertThat(payment.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.failureCode()).isEqualTo("SHIPMENT_FAILED");
        assertThat(payment.capturedAmount()).isEqualTo(Money.vnd(0));

        assertThat(allocationRepository.allocations).isEmpty();
        assertThat(ledgerRepository.postings).isEmpty();

        assertThat(outbox.events)
                .filteredOn(PaymentFailedEvent.class::isInstance)
                .hasSize(1);

        assertThat(outbox.events)
                .noneMatch(WalletAllocatedEvent.class::isInstance);
    }

    @Test
    void shouldApplyCancelledSingleOrderCodPayment() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        Payment payment = codPayment(shopId, Money.vnd(100_000));

        paymentRepository.add(payment);

        UUID orderId = payment.orders().getFirst().orderId().value();

        ProcessCodPaymentResult result = service.execute(new ProcessCodPaymentCommand(
                orderId,
                CodPaymentResultStatus.CANCELLED,
                OCCURRED_AT,
                "ORDER_CANCELLED"
        ));

        assertThat(result.action()).isEqualTo(CodPaymentProcessingAction.APPLIED);
        assertThat(result.paymentStatus()).isEqualTo("FAILED");
        assertThat(result.orderCodStatus()).isEqualTo("FAILED");
        assertThat(payment.failureCode()).isEqualTo("ORDER_CANCELLED");
        assertThat(payment.order(new OrderId(orderId)).codFailureCode()).isEqualTo("ORDER_CANCELLED");

        assertThat(allocationRepository.allocations).isEmpty();
        assertThat(ledgerRepository.postings).isEmpty();

        assertThat(outbox.events)
                .filteredOn(PaymentFailedEvent.class::isInstance)
                .hasSize(1);
    }

    @Test
    void shouldRejectUnknownOrder() {
        assertThatThrownBy(() -> service.execute(new ProcessCodPaymentCommand(
                UUID.randomUUID(),
                CodPaymentResultStatus.DELIVERED,
                OCCURRED_AT,
                null
        ))).isInstanceOf(PaymentNotFoundByOrderException.class);
    }

    @Test
    void shouldRejectNonCodPayment() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        Payment payment = vnpayPayment(shopId, Money.vnd(100_000));

        paymentRepository.add(payment);

        UUID orderId = payment.orders().getFirst().orderId().value();

        assertThatThrownBy(() -> service.execute(new ProcessCodPaymentCommand(
                orderId,
                CodPaymentResultStatus.DELIVERED,
                OCCURRED_AT,
                null
        ))).isInstanceOf(UnsupportedPaymentMethodException.class);
    }

    @Test
    void shouldCaptureOnlyDeliveredChildOrder() {
        ShopId firstShopId = new ShopId(UUID.randomUUID());
        ShopId secondShopId = new ShopId(UUID.randomUUID());

        PaymentOrder firstOrder = new PaymentOrder(
                new OrderId(UUID.randomUUID()),
                firstShopId,
                Money.vnd(90_000),
                Money.vnd(10_000)
        );

        PaymentOrder secondOrder = new PaymentOrder(
                new OrderId(UUID.randomUUID()),
                secondShopId,
                Money.vnd(180_000),
                Money.vnd(20_000)
        );

        Payment payment = Payment.create(
                new PaymentId(UUID.randomUUID()),
                new CheckoutGroupId(UUID.randomUUID()),
                new BuyerUserId(UUID.randomUUID()),
                PaymentMethod.COD,
                Money.vnd(300_000),
                List.of(firstOrder, secondOrder)
        );

        payment.clearDomainEvents();

        Wallet firstWallet = Wallet.create(new WalletId(UUID.randomUUID()), firstShopId);
        Wallet secondWallet = Wallet.create(new WalletId(UUID.randomUUID()), secondShopId);

        paymentRepository.add(payment);
        walletRepository.add(firstWallet);
        walletRepository.add(secondWallet);

        ProcessCodPaymentResult result = service.execute(new ProcessCodPaymentCommand(
                firstOrder.orderId().value(),
                CodPaymentResultStatus.DELIVERED,
                OCCURRED_AT,
                null
        ));

        assertThat(result.action()).isEqualTo(CodPaymentProcessingAction.APPLIED);
        assertThat(result.paymentStatus()).isEqualTo("PENDING_COD");
        assertThat(result.orderCodStatus()).isEqualTo("CAPTURED");

        assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING_COD);
        assertThat(payment.capturedAmount()).isEqualTo(Money.vnd(100_000));

        assertThat(payment.order(firstOrder.orderId()).codStatus())
                .isEqualTo(PaymentOrderCodStatus.CAPTURED);

        assertThat(payment.order(secondOrder.orderId()).codStatus())
                .isEqualTo(PaymentOrderCodStatus.PENDING);

        assertThat(allocationRepository.allocations).hasSize(1);
        assertThat(allocationRepository.allocations.getFirst().orderId())
                .isEqualTo(firstOrder.orderId());

        assertThat(firstWallet.pendingBalance()).isEqualTo(Money.vnd(82_800));
        assertThat(secondWallet.pendingBalance()).isEqualTo(Money.vnd(0));

        assertThat(ledgerRepository.postings).hasSize(1);
        assertThat(ledgerRepository.postings.getFirst().postingType()).isEqualTo("COD_CAPTURE");

        assertThat(outbox.events).anyMatch(WalletAllocatedEvent.class::isInstance);
        assertThat(outbox.events).noneMatch(PaymentSucceededEvent.class::isInstance);
    }

    @Test
    void shouldMarkPaymentSucceededOnlyAfterAllChildOrdersAreCaptured() {
        ShopId firstShopId = new ShopId(UUID.randomUUID());
        ShopId secondShopId = new ShopId(UUID.randomUUID());

        PaymentOrder firstOrder = new PaymentOrder(
                new OrderId(UUID.randomUUID()),
                firstShopId,
                Money.vnd(100_000)
        );

        PaymentOrder secondOrder = new PaymentOrder(
                new OrderId(UUID.randomUUID()),
                secondShopId,
                Money.vnd(200_000)
        );

        Payment payment = Payment.create(
                new PaymentId(UUID.randomUUID()),
                new CheckoutGroupId(UUID.randomUUID()),
                new BuyerUserId(UUID.randomUUID()),
                PaymentMethod.COD,
                Money.vnd(300_000),
                List.of(firstOrder, secondOrder)
        );

        payment.clearDomainEvents();

        walletRepository.add(Wallet.create(new WalletId(UUID.randomUUID()), firstShopId));
        walletRepository.add(Wallet.create(new WalletId(UUID.randomUUID()), secondShopId));
        paymentRepository.add(payment);

        service.execute(new ProcessCodPaymentCommand(
                firstOrder.orderId().value(),
                CodPaymentResultStatus.DELIVERED,
                OCCURRED_AT,
                null
        ));

        assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING_COD);
        assertThat(payment.capturedAmount()).isEqualTo(Money.vnd(100_000));
        assertThat(outbox.events).noneMatch(PaymentSucceededEvent.class::isInstance);

        service.execute(new ProcessCodPaymentCommand(
                secondOrder.orderId().value(),
                CodPaymentResultStatus.DELIVERED,
                OCCURRED_AT.plusSeconds(3600),
                null
        ));

        assertThat(payment.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.capturedAmount()).isEqualTo(Money.vnd(300_000));

        assertThat(payment.order(firstOrder.orderId()).codStatus())
                .isEqualTo(PaymentOrderCodStatus.CAPTURED);

        assertThat(payment.order(secondOrder.orderId()).codStatus())
                .isEqualTo(PaymentOrderCodStatus.CAPTURED);

        assertThat(allocationRepository.allocations).hasSize(2);
        assertThat(ledgerRepository.postings).hasSize(2);

        assertThat(ledgerRepository.postings)
                .extracting(LedgerPosting::businessKey)
                .containsExactlyInAnyOrder(
                        "COD_CAPTURE:" + payment.id().value() + ":" + firstOrder.orderId().value(),
                        "COD_CAPTURE:" + payment.id().value() + ":" + secondOrder.orderId().value()
                );

        assertThat(outbox.events)
                .filteredOn(PaymentSucceededEvent.class::isInstance)
                .hasSize(1);

        assertThat(outbox.events)
                .filteredOn(WalletAllocatedEvent.class::isInstance)
                .hasSize(2);
    }

    @Test
    void shouldIgnoreDuplicateDeliveredChildOrder() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        Payment payment = codPayment(shopId, Money.vnd(100_000));
        Wallet wallet = Wallet.create(new WalletId(UUID.randomUUID()), shopId);

        paymentRepository.add(payment);
        walletRepository.add(wallet);

        UUID orderId = payment.orders().getFirst().orderId().value();

        ProcessCodPaymentCommand command = new ProcessCodPaymentCommand(
                orderId,
                CodPaymentResultStatus.DELIVERED,
                OCCURRED_AT,
                null
        );

        ProcessCodPaymentResult first = service.execute(command);
        ProcessCodPaymentResult duplicate = service.execute(command);

        assertThat(first.action()).isEqualTo(CodPaymentProcessingAction.APPLIED);
        assertThat(duplicate.action()).isEqualTo(CodPaymentProcessingAction.DUPLICATE);
        assertThat(duplicate.paymentStatus()).isEqualTo("SUCCESS");
        assertThat(duplicate.orderCodStatus()).isEqualTo("CAPTURED");

        assertThat(allocationRepository.allocations).hasSize(1);
        assertThat(ledgerRepository.postings).hasSize(1);
        assertThat(wallet.pendingBalance()).isEqualTo(Money.vnd(92_000));

        assertThat(outbox.events)
                .filteredOn(PaymentSucceededEvent.class::isInstance)
                .hasSize(1);

        assertThat(outbox.events)
                .filteredOn(WalletAllocatedEvent.class::isInstance)
                .hasSize(1);
    }

    @Test
    void shouldIgnoreDuplicateFailedChildOrder() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        Payment payment = codPayment(shopId, Money.vnd(100_000));

        paymentRepository.add(payment);

        UUID orderId = payment.orders().getFirst().orderId().value();

        ProcessCodPaymentCommand command = new ProcessCodPaymentCommand(
                orderId,
                CodPaymentResultStatus.FAILED,
                OCCURRED_AT,
                "SHIPMENT_FAILED"
        );

        ProcessCodPaymentResult first = service.execute(command);
        ProcessCodPaymentResult duplicate = service.execute(command);

        assertThat(first.action()).isEqualTo(CodPaymentProcessingAction.APPLIED);
        assertThat(duplicate.action()).isEqualTo(CodPaymentProcessingAction.DUPLICATE);
        assertThat(duplicate.paymentStatus()).isEqualTo("FAILED");
        assertThat(duplicate.orderCodStatus()).isEqualTo("FAILED");

        assertThat(allocationRepository.allocations).isEmpty();
        assertThat(ledgerRepository.postings).isEmpty();

        assertThat(outbox.events)
                .filteredOn(PaymentFailedEvent.class::isInstance)
                .hasSize(1);
    }

    @Test
    void shouldKeepPaymentPendingWhenChildOutcomesAreMixed() {
        ShopId firstShopId = new ShopId(UUID.randomUUID());
        ShopId secondShopId = new ShopId(UUID.randomUUID());

        PaymentOrder firstOrder = new PaymentOrder(
                new OrderId(UUID.randomUUID()),
                firstShopId,
                Money.vnd(100_000)
        );

        PaymentOrder secondOrder = new PaymentOrder(
                new OrderId(UUID.randomUUID()),
                secondShopId,
                Money.vnd(200_000)
        );

        Payment payment = Payment.create(
                new PaymentId(UUID.randomUUID()),
                new CheckoutGroupId(UUID.randomUUID()),
                new BuyerUserId(UUID.randomUUID()),
                PaymentMethod.COD,
                Money.vnd(300_000),
                List.of(firstOrder, secondOrder)
        );

        payment.clearDomainEvents();

        walletRepository.add(Wallet.create(new WalletId(UUID.randomUUID()), firstShopId));
        paymentRepository.add(payment);

        service.execute(new ProcessCodPaymentCommand(
                firstOrder.orderId().value(),
                CodPaymentResultStatus.DELIVERED,
                OCCURRED_AT,
                null
        ));

        service.execute(new ProcessCodPaymentCommand(
                secondOrder.orderId().value(),
                CodPaymentResultStatus.FAILED,
                OCCURRED_AT.plusSeconds(3600),
                "SHIPMENT_FAILED"
        ));

        assertThat(payment.order(firstOrder.orderId()).codStatus())
                .isEqualTo(PaymentOrderCodStatus.CAPTURED);

        assertThat(payment.order(secondOrder.orderId()).codStatus())
                .isEqualTo(PaymentOrderCodStatus.FAILED);

        assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING_COD);
        assertThat(payment.capturedAmount()).isEqualTo(Money.vnd(100_000));

        assertThat(allocationRepository.allocations).hasSize(1);
        assertThat(ledgerRepository.postings).hasSize(1);

        assertThat(outbox.events)
                .noneMatch(PaymentSucceededEvent.class::isInstance);

        assertThat(outbox.events)
                .noneMatch(PaymentFailedEvent.class::isInstance);
    }

    @Test
    void shouldMarkPaymentFailedWhenAllChildOrdersFail() {
        ShopId firstShopId = new ShopId(UUID.randomUUID());
        ShopId secondShopId = new ShopId(UUID.randomUUID());

        PaymentOrder firstOrder = new PaymentOrder(
                new OrderId(UUID.randomUUID()),
                firstShopId,
                Money.vnd(100_000)
        );

        PaymentOrder secondOrder = new PaymentOrder(
                new OrderId(UUID.randomUUID()),
                secondShopId,
                Money.vnd(200_000)
        );

        Payment payment = Payment.create(
                new PaymentId(UUID.randomUUID()),
                new CheckoutGroupId(UUID.randomUUID()),
                new BuyerUserId(UUID.randomUUID()),
                PaymentMethod.COD,
                Money.vnd(300_000),
                List.of(firstOrder, secondOrder)
        );

        payment.clearDomainEvents();
        paymentRepository.add(payment);

        service.execute(new ProcessCodPaymentCommand(
                firstOrder.orderId().value(),
                CodPaymentResultStatus.FAILED,
                OCCURRED_AT,
                "SHIPMENT_FAILED"
        ));

        assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING_COD);

        service.execute(new ProcessCodPaymentCommand(
                secondOrder.orderId().value(),
                CodPaymentResultStatus.FAILED,
                OCCURRED_AT.plusSeconds(3600),
                "SHIPMENT_FAILED"
        ));

        assertThat(payment.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.capturedAmount()).isEqualTo(Money.vnd(0));

        assertThat(payment.order(firstOrder.orderId()).codStatus())
                .isEqualTo(PaymentOrderCodStatus.FAILED);

        assertThat(payment.order(secondOrder.orderId()).codStatus())
                .isEqualTo(PaymentOrderCodStatus.FAILED);

        assertThat(outbox.events)
                .filteredOn(PaymentFailedEvent.class::isInstance)
                .hasSize(1);
    }

    @Test
    void shouldIgnoreFailedEventAfterOrderWasAlreadyCaptured() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        Payment payment = codPayment(shopId, Money.vnd(100_000));
        Wallet wallet = Wallet.create(new WalletId(UUID.randomUUID()), shopId);

        paymentRepository.add(payment);
        walletRepository.add(wallet);

        UUID orderId = payment.orders().getFirst().orderId().value();

        ProcessCodPaymentResult delivered = service.execute(
                new ProcessCodPaymentCommand(
                        orderId,
                        CodPaymentResultStatus.DELIVERED,
                        OCCURRED_AT,
                        null
                )
        );

        ProcessCodPaymentResult lateFailed = service.execute(
                new ProcessCodPaymentCommand(
                        orderId,
                        CodPaymentResultStatus.FAILED,
                        OCCURRED_AT.plusSeconds(60),
                        "SHIPMENT_FAILED"
                )
        );

        assertThat(delivered.action())
                .isEqualTo(CodPaymentProcessingAction.APPLIED);

        assertThat(lateFailed.action())
                .isEqualTo(CodPaymentProcessingAction.DUPLICATE);

        assertThat(payment.order(new OrderId(orderId)).codStatus())
                .isEqualTo(PaymentOrderCodStatus.CAPTURED);

        assertThat(payment.status())
                .isEqualTo(PaymentStatus.SUCCESS);

        assertThat(allocationRepository.allocations)
                .hasSize(1);

        assertThat(ledgerRepository.postings)
                .hasSize(1);
    }

    @Test
    void shouldIgnoreDeliveredEventAfterOrderWasAlreadyFailed() {
        ShopId shopId = new ShopId(UUID.randomUUID());
        Payment payment = codPayment(shopId, Money.vnd(100_000));

        paymentRepository.add(payment);

        UUID orderId = payment.orders().getFirst().orderId().value();

        ProcessCodPaymentResult failed = service.execute(
                new ProcessCodPaymentCommand(
                        orderId,
                        CodPaymentResultStatus.FAILED,
                        OCCURRED_AT,
                        "SHIPMENT_FAILED"
                )
        );

        ProcessCodPaymentResult lateDelivered = service.execute(
                new ProcessCodPaymentCommand(
                        orderId,
                        CodPaymentResultStatus.DELIVERED,
                        OCCURRED_AT.plusSeconds(60),
                        null
                )
        );

        assertThat(failed.action())
                .isEqualTo(CodPaymentProcessingAction.APPLIED);

        assertThat(lateDelivered.action())
                .isEqualTo(CodPaymentProcessingAction.DUPLICATE);

        assertThat(payment.order(new OrderId(orderId)).codStatus())
                .isEqualTo(PaymentOrderCodStatus.FAILED);

        assertThat(payment.status())
                .isEqualTo(PaymentStatus.FAILED);

        assertThat(allocationRepository.allocations)
                .isEmpty();

        assertThat(ledgerRepository.postings)
                .isEmpty();
    }

    private Payment codPayment(ShopId shopId, Money amount) {
        Payment payment = Payment.create(
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

        payment.clearDomainEvents();
        return payment;
    }

    private Payment vnpayPayment(ShopId shopId, Money amount) {
        Payment payment = Payment.create(
                new PaymentId(UUID.randomUUID()),
                new CheckoutGroupId(UUID.randomUUID()),
                new BuyerUserId(UUID.randomUUID()),
                PaymentMethod.VNPAY,
                amount,
                List.of(new PaymentOrder(
                        new OrderId(UUID.randomUUID()),
                        shopId,
                        amount
                )),
                OCCURRED_AT.plusSeconds(900)
        );

        payment.clearDomainEvents();
        return payment;
    }

    private static final class FakePaymentRepositoryPort implements PaymentRepositoryPort {

        private final Map<PaymentId, Payment> paymentsById = new HashMap<>();
        private final Map<CheckoutGroupId, Payment> paymentsByCheckoutGroupId = new HashMap<>();
        private final Map<OrderId, Payment> paymentsByOrderId = new HashMap<>();

        void add(Payment payment) {
            paymentsById.put(payment.id(), payment);
            paymentsByCheckoutGroupId.put(payment.checkoutGroupId(), payment);
            payment.orders().forEach(order -> paymentsByOrderId.put(order.orderId(), payment));
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
        public Optional<Payment> findByOrderId(OrderId orderId) {
            return Optional.ofNullable(paymentsByOrderId.get(orderId));
        }

        @Override
        public Optional<Payment> findByOrderIdForUpdate(OrderId orderId) {
            return findByOrderId(orderId);
        }

        @Override
        public Payment save(Payment payment) {
            add(payment);
            return payment;
        }
    }

    private static final class FakePaymentAllocationRepositoryPort
            implements PaymentAllocationRepositoryPort {

        private final List<PaymentAllocation> allocations = new ArrayList<>();

        @Override
        public void saveAll(List<PaymentAllocation> allocations) {
            this.allocations.addAll(allocations);
        }

        @Override
        public List<PaymentAllocation> findByPaymentId(PaymentId paymentId) {
            return allocations.stream()
                    .filter(allocation -> allocation.paymentId().equals(paymentId))
                    .toList();
        }
    }

    private static final class FakeWalletRepositoryPort implements WalletRepositoryPort {

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
            return findById(walletId);
        }

        @Override
        public Optional<Wallet> findByShopIdAndCurrencyForUpdate(ShopId shopId, String currency) {
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

    private static final class FakeLedgerPostingRepositoryPort
            implements LedgerPostingRepositoryPort {

        private final List<LedgerPosting> postings = new ArrayList<>();

        @Override
        public void save(LedgerPosting posting) {
            postings.add(posting);
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
        public Map<ShopId, LedgerAccountId> sellerPendingAccountsFor(List<ShopId> shopIds) {
            Map<ShopId, LedgerAccountId> result = new HashMap<>();
            shopIds.forEach(shopId -> result.put(shopId, new LedgerAccountId(UUID.randomUUID())));
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

        @Override
        public LedgerAccountId codClearingAccount() {
            return new LedgerAccountId(UUID.randomUUID());
        }
    }

    private static final class FakeFeePolicyPort implements FeePolicyPort {

        @Override
        public PaymentFeePolicy currentPaymentFeePolicy() {
            return new PaymentFeePolicy(
                    new FeeConfigId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                    new TaxConfigId(UUID.fromString("22222222-2222-2222-2222-222222222222")),
                    RateBps.of(700),
                    RateBps.of(100)
            );
        }
    }

    private static final class FakeOutboxPort implements OutboxPort {

        private final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void save(DomainEvent event) {
            events.add(event);
        }
    }

    private static final class FakeTransactionPort implements TransactionPort {

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
            return new PaymentAttemptId(UUID.randomUUID());
        }
    }
}