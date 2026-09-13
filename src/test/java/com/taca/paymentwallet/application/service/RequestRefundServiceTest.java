package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.CreatePaymentCommand;
import com.taca.paymentwallet.application.command.RequestPayoutCommand;
import com.taca.paymentwallet.application.command.RequestRefundCommand;
import com.taca.paymentwallet.application.exception.IdempotencyKeyReuseException;
import com.taca.paymentwallet.application.exception.PaymentNotFoundException;
import com.taca.paymentwallet.application.exception.RequestAlreadyProcessingException;
import com.taca.paymentwallet.application.idempotency.IdempotencyRecord;
import com.taca.paymentwallet.application.idempotency.IdempotencyScope;
import com.taca.paymentwallet.application.idempotency.IdempotencyStatus;
import com.taca.paymentwallet.application.port.out.IdGeneratorPort;
import com.taca.paymentwallet.application.port.out.IdempotencyPort;
import com.taca.paymentwallet.application.port.out.OutboxPort;
import com.taca.paymentwallet.application.port.out.PaymentRepositoryPort;
import com.taca.paymentwallet.application.port.out.RefundRepositoryPort;
import com.taca.paymentwallet.application.port.out.RequestHashPort;
import com.taca.paymentwallet.application.port.out.RequestRefundResultPayloadPort;
import com.taca.paymentwallet.application.port.out.TransactionPort;
import com.taca.paymentwallet.application.result.RequestRefundResult;
import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.payment.PaymentMethod;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.refund.Refund;
import com.taca.paymentwallet.domain.refund.RefundLimitExceededException;
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
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestRefundServiceTest {

    @Test
    void shouldRequestRefund() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        RefundId refundId = new RefundId(UUID.randomUUID());

        FakePaymentRepositoryPort paymentRepository =
                new FakePaymentRepositoryPort(succeededPayment(paymentId));

        FakeRefundRepositoryPort refundRepository =
                new FakeRefundRepositoryPort(Money.vnd(0));

        FakeIdempotencyPort idempotencyPort = new FakeIdempotencyPort();
        FakeOutboxPort outboxPort = new FakeOutboxPort();

        RequestRefundService service = newService(
                paymentRepository,
                refundRepository,
                idempotencyPort,
                outboxPort,
                new FixedIdGeneratorPort(refundId)
        );

        RequestRefundResult result = service.execute(refundCommand(paymentId.value(), 50_000));

        assertEquals(refundId.value(), result.refundId());
        assertEquals(paymentId.value(), result.paymentId());
        assertEquals(50_000, result.amount());
        assertEquals("VND", result.currency());
        assertEquals("REQUESTED", result.status());

        assertEquals(1, refundRepository.savedRefunds.size());
        assertEquals(IdempotencyStatus.SUCCEEDED, idempotencyPort.record.status());
        assertTrue(outboxPort.events.stream()
                .anyMatch(event -> event.eventType().equals("refund.requested")));
    }

    @Test
    void shouldReturnPreviousResultWhenIdempotencySucceeded() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        RefundId existingRefundId = new RefundId(UUID.randomUUID());

        FakeIdempotencyPort idempotencyPort = new FakeIdempotencyPort();
        idempotencyPort.record = new IdempotencyRecord(
                IdempotencyScope.refund(paymentId),
                "refund-idem-key-1",
                "refund-request-hash",
                IdempotencyStatus.SUCCEEDED,
                existingRefundId.value() + "|" + paymentId.value() + "|50_000|VND|REQUESTED",
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );

        FakeRefundRepositoryPort refundRepository =
                new FakeRefundRepositoryPort(Money.vnd(0));

        RequestRefundService service = newService(
                new FakePaymentRepositoryPort(succeededPayment(paymentId)),
                refundRepository,
                idempotencyPort,
                new FakeOutboxPort(),
                new FixedIdGeneratorPort(new RefundId(UUID.randomUUID()))
        );

        RequestRefundResult result = service.execute(refundCommand(paymentId.value(), 50_000));

        assertEquals(existingRefundId.value(), result.refundId());
        assertEquals(paymentId.value(), result.paymentId());
        assertEquals(50_000, result.amount());
        assertEquals("REQUESTED", result.status());
        assertEquals(0, refundRepository.savedRefunds.size());
    }

    @Test
    void shouldRejectSameIdempotencyKeyWithDifferentRequestHash() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());

        FakeIdempotencyPort idempotencyPort = new FakeIdempotencyPort();
        idempotencyPort.record = new IdempotencyRecord(
                IdempotencyScope.refund(paymentId),
                "refund-idem-key-1",
                "different-request-hash",
                IdempotencyStatus.SUCCEEDED,
                UUID.randomUUID() + "|" + paymentId.value() + "|50_000|VND|REQUESTED",
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );

        RequestRefundService service = newService(
                new FakePaymentRepositoryPort(succeededPayment(paymentId)),
                new FakeRefundRepositoryPort(Money.vnd(0)),
                idempotencyPort,
                new FakeOutboxPort(),
                new FixedIdGeneratorPort(new RefundId(UUID.randomUUID()))
        );

        assertThrows(
                IdempotencyKeyReuseException.class,
                () -> service.execute(refundCommand(paymentId.value(), 50_000))
        );
    }

    @Test
    void shouldRejectProcessingIdempotencyRequest() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());

        FakeIdempotencyPort idempotencyPort = new FakeIdempotencyPort();
        idempotencyPort.record = new IdempotencyRecord(
                IdempotencyScope.refund(paymentId),
                "refund-idem-key-1",
                "refund-request-hash",
                IdempotencyStatus.PROCESSING,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );

        RequestRefundService service = newService(
                new FakePaymentRepositoryPort(succeededPayment(paymentId)),
                new FakeRefundRepositoryPort(Money.vnd(0)),
                idempotencyPort,
                new FakeOutboxPort(),
                new FixedIdGeneratorPort(new RefundId(UUID.randomUUID()))
        );

        assertThrows(
                RequestAlreadyProcessingException.class,
                () -> service.execute(refundCommand(paymentId.value(), 50_000))
        );
    }

    @Test
    void shouldRejectUnknownPayment() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());

        RequestRefundService service = newService(
                new FakePaymentRepositoryPort(null),
                new FakeRefundRepositoryPort(Money.vnd(0)),
                new FakeIdempotencyPort(),
                new FakeOutboxPort(),
                new FixedIdGeneratorPort(new RefundId(UUID.randomUUID()))
        );

        assertThrows(
                PaymentNotFoundException.class,
                () -> service.execute(refundCommand(paymentId.value(), 50_000))
        );
    }

    @Test
    void shouldRejectRefundAmountGreaterThanCapturedAmount() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());

        RequestRefundService service = newService(
                new FakePaymentRepositoryPort(succeededPayment(paymentId)),
                new FakeRefundRepositoryPort(Money.vnd(0)),
                new FakeIdempotencyPort(),
                new FakeOutboxPort(),
                new FixedIdGeneratorPort(new RefundId(UUID.randomUUID()))
        );

        assertThrows(
                RefundLimitExceededException.class,
                () -> service.execute(refundCommand(paymentId.value(), 150_000))
        );
    }

    private RequestRefundService newService(
            FakePaymentRepositoryPort paymentRepository,
            FakeRefundRepositoryPort refundRepository,
            FakeIdempotencyPort idempotencyPort,
            FakeOutboxPort outboxPort,
            IdGeneratorPort idGeneratorPort
    ) {
        return new RequestRefundService(
                paymentRepository,
                refundRepository,
                idempotencyPort,
                new FakeRequestHashPort(),
                idGeneratorPort,
                outboxPort,
                new ImmediateTransactionPort(),
                new PipeSeparatedRequestRefundResultPayloadPort()
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
                ))
        );

        payment.markSucceeded(Instant.parse("2026-01-01T00:00:00Z"));
        payment.clearDomainEvents();

        return payment;
    }

    private RequestRefundCommand refundCommand(UUID paymentId, long amount) {
        return new RequestRefundCommand(
                paymentId,
                amount,
                "VND",
                "Buyer requested refund",
                "refund-idem-key-1"
        );
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

    private static final class FakeRefundRepositoryPort implements RefundRepositoryPort {

        private final List<Refund> savedRefunds = new ArrayList<>();
        private final Money pendingRefundAmount;

        private FakeRefundRepositoryPort(Money pendingRefundAmount) {
            this.pendingRefundAmount = pendingRefundAmount;
        }

        @Override
        public Optional<Refund> findById(RefundId refundId) {
            return Optional.empty();
        }

        @Override
        public Money sumPendingRefundAmount(PaymentId paymentId) {
            return pendingRefundAmount;
        }

        @Override
        public Refund save(Refund refund) {
            savedRefunds.add(refund);
            return refund;
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

        private final RefundId refundId;

        private FixedIdGeneratorPort(RefundId refundId) {
            this.refundId = refundId;
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
            return refundId;
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

    private static final class PipeSeparatedRequestRefundResultPayloadPort
            implements RequestRefundResultPayloadPort {

        @Override
        public String serialize(RequestRefundResult result) {
            return result.refundId()
                    + "|"
                    + result.paymentId()
                    + "|"
                    + result.amount()
                    + "|"
                    + result.currency()
                    + "|"
                    + result.status();
        }

        @Override
        public RequestRefundResult deserialize(String payload) {
            String[] parts = payload.split("\\|", -1);

            return new RequestRefundResult(
                    UUID.fromString(parts[0]),
                    UUID.fromString(parts[1]),
                    Long.parseLong(parts[2].replace("_", "")),
                    parts[3],
                    parts[4]
            );
        }
    }
}
