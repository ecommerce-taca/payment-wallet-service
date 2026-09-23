package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.ProcessRefundResultCommand;
import com.taca.paymentwallet.application.exception.PaymentNotFoundException;
import com.taca.paymentwallet.application.exception.RefundAmountMismatchException;
import com.taca.paymentwallet.application.exception.RefundNotFoundException;
import com.taca.paymentwallet.application.port.out.IdGeneratorPort;
import com.taca.paymentwallet.application.port.out.LedgerAccountLookupPort;
import com.taca.paymentwallet.application.port.out.LedgerPostingRepositoryPort;
import com.taca.paymentwallet.application.port.out.OutboxPort;
import com.taca.paymentwallet.application.port.out.PaymentAllocationRepositoryPort;
import com.taca.paymentwallet.application.port.out.PaymentRepositoryPort;
import com.taca.paymentwallet.application.port.out.RefundAllocationRepositoryPort;
import com.taca.paymentwallet.application.port.out.RefundRepositoryPort;
import com.taca.paymentwallet.application.port.out.TransactionPort;
import com.taca.paymentwallet.application.refund.RefundResultProcessingAction;
import com.taca.paymentwallet.application.refund.RefundResultStatus;
import com.taca.paymentwallet.application.result.ProcessRefundResult;
import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.finance.RefundAllocationCalculator;
import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.payment.PaymentMethod;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.payment.PaymentStatus;
import com.taca.paymentwallet.domain.refund.Refund;
import com.taca.paymentwallet.domain.refund.RefundAllocation;
import com.taca.paymentwallet.domain.refund.RefundStatus;
import com.taca.paymentwallet.domain.valueobject.BuyerUserId;
import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.IdempotencyKey;
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
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessRefundResultServiceTest {

    @Test
    void shouldApplySuccessfulRefundResult() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        RefundId refundId = new RefundId(UUID.randomUUID());
        PaymentAllocationId allocationId = new PaymentAllocationId(UUID.randomUUID());

        Payment payment = succeededPayment(paymentId);
        Refund refund = requestedRefund(refundId, paymentId, Money.vnd(50_000));

        FakeRefundRepositoryPort refundRepository =
                new FakeRefundRepositoryPort(refund);

        FakePaymentRepositoryPort paymentRepository =
                new FakePaymentRepositoryPort(payment);

        FakePaymentAllocationRepositoryPort paymentAllocationRepository =
                new FakePaymentAllocationRepositoryPort(List.of(paymentAllocation(allocationId)));

        FakeRefundAllocationRepositoryPort refundAllocationRepository =
                new FakeRefundAllocationRepositoryPort();

        FakeLedgerPostingRepositoryPort ledgerPostingRepository =
                new FakeLedgerPostingRepositoryPort();

        FakeOutboxPort outboxPort = new FakeOutboxPort();

        ProcessRefundResultService service = newService(
                refundRepository,
                paymentRepository,
                paymentAllocationRepository,
                refundAllocationRepository,
                ledgerPostingRepository,
                outboxPort
        );

        ProcessRefundResult result = service.execute(successCommand(refundId.value(), 50_000));

        assertEquals(refundId.value(), result.refundId());
        assertEquals(paymentId.value(), result.paymentId());
        assertEquals("SUCCESS", result.refundStatus());
        assertEquals("PARTIALLY_REFUNDED", result.paymentStatus());
        assertEquals(RefundResultProcessingAction.APPLIED, result.action());

        assertEquals(RefundStatus.SUCCESS, refundRepository.refund.status());
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, paymentRepository.payment.status());
        assertEquals(Money.vnd(50_000), paymentRepository.payment.refundedAmount());

        assertEquals(1, refundAllocationRepository.savedRefundAllocations.size());
        assertEquals(1, ledgerPostingRepository.savedPostings.size());
        assertEquals("REFUND_SUCCESS", ledgerPostingRepository.savedPostings.getFirst().postingType());

        assertTrue(outboxPort.events.stream()
                .anyMatch(event -> event.eventType().equals("refund.succeeded")));
        assertTrue(outboxPort.events.stream()
                .anyMatch(event -> event.eventType().equals("payment.refunded")));
    }

    @Test
    void shouldApplyFailedRefundResult() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        RefundId refundId = new RefundId(UUID.randomUUID());

        Refund refund = requestedRefund(refundId, paymentId, Money.vnd(50_000));

        FakeRefundRepositoryPort refundRepository =
                new FakeRefundRepositoryPort(refund);

        FakePaymentRepositoryPort paymentRepository =
                new FakePaymentRepositoryPort(succeededPayment(paymentId));

        FakeRefundAllocationRepositoryPort refundAllocationRepository =
                new FakeRefundAllocationRepositoryPort();

        FakeLedgerPostingRepositoryPort ledgerPostingRepository =
                new FakeLedgerPostingRepositoryPort();

        FakeOutboxPort outboxPort = new FakeOutboxPort();

        ProcessRefundResultService service = newService(
                refundRepository,
                paymentRepository,
                new FakePaymentAllocationRepositoryPort(List.of()),
                refundAllocationRepository,
                ledgerPostingRepository,
                outboxPort
        );

        ProcessRefundResult result = service.execute(failedCommand(refundId.value(), 50_000));

        assertEquals(refundId.value(), result.refundId());
        assertEquals(paymentId.value(), result.paymentId());
        assertEquals("FAILED", result.refundStatus());
        assertEquals("SUCCESS", result.paymentStatus());
        assertEquals(RefundResultProcessingAction.APPLIED, result.action());

        assertEquals(RefundStatus.FAILED, refundRepository.refund.status());
        assertEquals("VNPAY_REFUND_FAILED", refundRepository.refund.failureCode());

        assertEquals(0, refundAllocationRepository.savedRefundAllocations.size());
        assertEquals(0, ledgerPostingRepository.savedPostings.size());

        assertTrue(outboxPort.events.stream()
                .anyMatch(event -> event.eventType().equals("refund.failed")));
    }

    @Test
    void shouldIgnoreDuplicateSuccessfulRefundResult() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        RefundId refundId = new RefundId(UUID.randomUUID());

        Payment payment = succeededPayment(paymentId);
        payment.markRefundSucceeded(Money.vnd(50_000));
        payment.clearDomainEvents();

        Refund refund = requestedRefund(refundId, paymentId, Money.vnd(50_000));
        refund.markSucceeded();
        refund.clearDomainEvents();

        FakeRefundRepositoryPort refundRepository =
                new FakeRefundRepositoryPort(refund);

        FakePaymentRepositoryPort paymentRepository =
                new FakePaymentRepositoryPort(payment);

        FakeRefundAllocationRepositoryPort refundAllocationRepository =
                new FakeRefundAllocationRepositoryPort();

        FakeLedgerPostingRepositoryPort ledgerPostingRepository =
                new FakeLedgerPostingRepositoryPort();

        FakeOutboxPort outboxPort = new FakeOutboxPort();

        ProcessRefundResultService service = newService(
                refundRepository,
                paymentRepository,
                new FakePaymentAllocationRepositoryPort(List.of()),
                refundAllocationRepository,
                ledgerPostingRepository,
                outboxPort
        );

        ProcessRefundResult result = service.execute(successCommand(refundId.value(), 50_000));

        assertEquals(refundId.value(), result.refundId());
        assertEquals(paymentId.value(), result.paymentId());
        assertEquals("SUCCESS", result.refundStatus());
        assertEquals("PARTIALLY_REFUNDED", result.paymentStatus());
        assertEquals(RefundResultProcessingAction.DUPLICATE, result.action());

        assertEquals(0, refundAllocationRepository.savedRefundAllocations.size());
        assertEquals(0, ledgerPostingRepository.savedPostings.size());
        assertEquals(0, outboxPort.events.size());
    }

    @Test
    void shouldRejectUnknownRefund() {
        RefundId refundId = new RefundId(UUID.randomUUID());

        ProcessRefundResultService service = newService(
                new FakeRefundRepositoryPort(null),
                new FakePaymentRepositoryPort(null),
                new FakePaymentAllocationRepositoryPort(List.of()),
                new FakeRefundAllocationRepositoryPort(),
                new FakeLedgerPostingRepositoryPort(),
                new FakeOutboxPort()
        );

        assertThrows(
                RefundNotFoundException.class,
                () -> service.execute(successCommand(refundId.value(), 50_000))
        );
    }

    @Test
    void shouldRejectUnknownPayment() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        RefundId refundId = new RefundId(UUID.randomUUID());

        ProcessRefundResultService service = newService(
                new FakeRefundRepositoryPort(requestedRefund(refundId, paymentId, Money.vnd(50_000))),
                new FakePaymentRepositoryPort(null),
                new FakePaymentAllocationRepositoryPort(List.of()),
                new FakeRefundAllocationRepositoryPort(),
                new FakeLedgerPostingRepositoryPort(),
                new FakeOutboxPort()
        );

        assertThrows(
                PaymentNotFoundException.class,
                () -> service.execute(successCommand(refundId.value(), 50_000))
        );
    }

    @Test
    void shouldRejectRefundAmountMismatch() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        RefundId refundId = new RefundId(UUID.randomUUID());

        ProcessRefundResultService service = newService(
                new FakeRefundRepositoryPort(requestedRefund(refundId, paymentId, Money.vnd(50_000))),
                new FakePaymentRepositoryPort(succeededPayment(paymentId)),
                new FakePaymentAllocationRepositoryPort(List.of(paymentAllocation(new PaymentAllocationId(UUID.randomUUID())))),
                new FakeRefundAllocationRepositoryPort(),
                new FakeLedgerPostingRepositoryPort(),
                new FakeOutboxPort()
        );

        assertThrows(
                RefundAmountMismatchException.class,
                () -> service.execute(successCommand(refundId.value(), 40_000))
        );
    }

    private ProcessRefundResultService newService(
            FakeRefundRepositoryPort refundRepository,
            FakePaymentRepositoryPort paymentRepository,
            FakePaymentAllocationRepositoryPort paymentAllocationRepository,
            FakeRefundAllocationRepositoryPort refundAllocationRepository,
            FakeLedgerPostingRepositoryPort ledgerPostingRepository,
            FakeOutboxPort outboxPort
    ) {
        return new ProcessRefundResultService(
                refundRepository,
                paymentRepository,
                paymentAllocationRepository,
                refundAllocationRepository,
                ledgerPostingRepository,
                new FakeLedgerAccountLookupPort(),
                outboxPort,
                new ImmediateTransactionPort(),
                new FakeIdGeneratorPort(),
                new RefundAllocationCalculator(),
                new LedgerPostingFactory()
        );
    }

    private Payment succeededPayment(PaymentId paymentId) {
        Payment payment = Payment.create(
                paymentId,
                new CheckoutGroupId(UUID.randomUUID()),
                new BuyerUserId(UUID.randomUUID()),
                PaymentMethod.VNPAY,
                Money.vnd(100_000),
                List.of(new PaymentOrder(
                        new OrderId(UUID.randomUUID()),
                        new ShopId(UUID.randomUUID()),
                        Money.vnd(100_000)
                )),
                Instant.parse("2026-01-01T00:15:00Z")
        );

        payment.markSucceeded(Instant.parse("2026-01-01T00:00:00Z"));
        payment.clearDomainEvents();

        return payment;
    }

    private Refund requestedRefund(
            RefundId refundId,
            PaymentId paymentId,
            Money amount
    ) {
        Refund refund = Refund.request(
                refundId,
                paymentId,
                amount,
                "Buyer requested refund",
                new IdempotencyKey("refund-idem-key-1")
        );

        refund.clearDomainEvents();

        return refund;
    }

    private PaymentAllocation paymentAllocation(PaymentAllocationId allocationId) {
        return new PaymentAllocation(
                allocationId,
                new OrderId(UUID.randomUUID()),
                new ShopId(UUID.randomUUID()),
                Money.vnd(100_000),
                Money.vnd(7_000),
                Money.vnd(1_000),
                Money.vnd(92_000)
        );
    }

    private ProcessRefundResultCommand successCommand(UUID refundId, long amount) {
        return new ProcessRefundResultCommand(
                refundId,
                RefundResultStatus.SUCCESS,
                amount,
                "VND",
                "VNPAY-REFUND-001",
                null
        );
    }

    private ProcessRefundResultCommand failedCommand(UUID refundId, long amount) {
        return new ProcessRefundResultCommand(
                refundId,
                RefundResultStatus.FAILED,
                amount,
                "VND",
                null,
                "VNPAY_REFUND_FAILED"
        );
    }

    private static final class FakeRefundRepositoryPort implements RefundRepositoryPort {

        private Refund refund;

        private FakeRefundRepositoryPort(Refund refund) {
            this.refund = refund;
        }

        @Override
        public Optional<Refund> findById(RefundId refundId) {
            return Optional.ofNullable(refund);
        }

        @Override
        public Optional<Refund> findByIdForUpdate(RefundId refundId) {
            return Optional.ofNullable(refund);
        }

        @Override
        public Money sumPendingRefundAmount(PaymentId paymentId) {
            return Money.vnd(0);
        }

        @Override
        public Refund save(Refund refund) {
            this.refund = refund;
            return refund;
        }
    }

    private static final class FakePaymentRepositoryPort implements PaymentRepositoryPort {

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
            return payment;
        }
    }

    private static final class FakePaymentAllocationRepositoryPort
            implements PaymentAllocationRepositoryPort {

        private final List<PaymentAllocation> allocations;

        private FakePaymentAllocationRepositoryPort(List<PaymentAllocation> allocations) {
            this.allocations = List.copyOf(allocations);
        }

        @Override
        public void saveAll(List<PaymentAllocation> allocations) {
        }

        @Override
        public List<PaymentAllocation> findByPaymentId(PaymentId paymentId) {
            return allocations;
        }
    }

    private static final class FakeRefundAllocationRepositoryPort
            implements RefundAllocationRepositoryPort {

        private final List<RefundAllocation> savedRefundAllocations = new ArrayList<>();

        @Override
        public void saveAll(List<RefundAllocation> refundAllocations) {
            savedRefundAllocations.addAll(refundAllocations);
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
        public LedgerAccountId codClearingAccount() {
            return new LedgerAccountId(UUID.randomUUID());
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
