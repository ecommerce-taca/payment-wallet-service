package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.CreatePaymentCommand;
import com.taca.paymentwallet.application.command.CreatePaymentOrderCommand;
import com.taca.paymentwallet.application.command.RequestPayoutCommand;
import com.taca.paymentwallet.application.command.RequestRefundCommand;
import com.taca.paymentwallet.application.exception.IdempotencyKeyReuseException;
import com.taca.paymentwallet.application.exception.RequestAlreadyProcessingException;
import com.taca.paymentwallet.application.exception.UnsupportedPaymentMethodException;
import com.taca.paymentwallet.application.gateway.vnpay.CreateVnpayPaymentUrlRequest;
import com.taca.paymentwallet.application.gateway.vnpay.CreateVnpayPaymentUrlResult;
import com.taca.paymentwallet.application.idempotency.IdempotencyRecord;
import com.taca.paymentwallet.application.idempotency.IdempotencyScope;
import com.taca.paymentwallet.application.idempotency.IdempotencyStatus;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.CreatePaymentResultPayloadPort;
import com.taca.paymentwallet.application.port.out.IdGeneratorPort;
import com.taca.paymentwallet.application.port.out.IdempotencyPort;
import com.taca.paymentwallet.application.port.out.OutboxPort;
import com.taca.paymentwallet.application.port.out.PaymentRepositoryPort;
import com.taca.paymentwallet.application.port.out.RequestHashPort;
import com.taca.paymentwallet.application.port.out.TransactionPort;
import com.taca.paymentwallet.application.port.out.VnpayGatewayPort;
import com.taca.paymentwallet.application.result.CreatePaymentResult;
import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.LedgerAccountId;
import com.taca.paymentwallet.domain.valueobject.LedgerPostingId;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.PayoutId;
import com.taca.paymentwallet.domain.valueobject.RefundId;
import com.taca.paymentwallet.domain.valueobject.SettlementBatchId;
import com.taca.paymentwallet.domain.valueobject.SettlementBatchItemId;
import com.taca.paymentwallet.domain.valueobject.SettlementLineId;
import com.taca.paymentwallet.domain.valueobject.WalletId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreatePaymentServiceTest {

    @Test
    void shouldCreateVnpayPayment() {
        FakePaymentRepositoryPort paymentRepository = new FakePaymentRepositoryPort();
        FakeIdempotencyPort idempotencyPort = new FakeIdempotencyPort();
        FakeOutboxPort outboxPort = new FakeOutboxPort();

        UUID paymentUuid = UUID.randomUUID();

        CreatePaymentService service = newService(
                paymentRepository,
                idempotencyPort,
                outboxPort,
                new FixedIdGeneratorPort(paymentUuid),
                new FakeVnpayGatewayPort()
        );

        CreatePaymentResult result = service.execute(vnpayCommand());

        assertEquals(paymentUuid, result.paymentId());
        assertEquals("PENDING", result.status());
        assertEquals("https://sandbox.vnpay.vn/payment-url", result.paymentUrl());
        assertEquals(1, paymentRepository.savedPayments.size());
        assertEquals(IdempotencyStatus.SUCCEEDED, idempotencyPort.record.status());
    }

    @Test
    void shouldCreateCodPaymentWithoutPaymentUrl() {
        FakePaymentRepositoryPort paymentRepository = new FakePaymentRepositoryPort();
        FakeIdempotencyPort idempotencyPort = new FakeIdempotencyPort();
        FakeOutboxPort outboxPort = new FakeOutboxPort();

        UUID paymentUuid = UUID.randomUUID();

        CreatePaymentService service = newService(
                paymentRepository,
                idempotencyPort,
                outboxPort,
                new FixedIdGeneratorPort(paymentUuid),
                new FakeVnpayGatewayPort()
        );

        CreatePaymentResult result = service.execute(codCommand());

        assertEquals(paymentUuid, result.paymentId());
        assertEquals("PENDING_COD", result.status());
        assertNull(result.paymentUrl());
        assertEquals(1, paymentRepository.savedPayments.size());
    }

    @Test
    void shouldReturnPreviousResultWhenIdempotencySucceeded() {
        FakePaymentRepositoryPort paymentRepository = new FakePaymentRepositoryPort();
        FakeIdempotencyPort idempotencyPort = new FakeIdempotencyPort();
        FakeOutboxPort outboxPort = new FakeOutboxPort();

        UUID existingPaymentId = UUID.randomUUID();

        idempotencyPort.record = new IdempotencyRecord(
                IdempotencyScope.payment(new CheckoutGroupId(vnpayCommand().checkoutGroupId())),
                "idem-key-1",
                "request-hash",
                IdempotencyStatus.SUCCEEDED,
                existingPaymentId + "|PENDING|https://sandbox.vnpay.vn/old-payment-url",
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );

        CreatePaymentService service = newService(
                paymentRepository,
                idempotencyPort,
                outboxPort,
                new FixedIdGeneratorPort(UUID.randomUUID()),
                new FakeVnpayGatewayPort()
        );

        CreatePaymentResult result = service.execute(vnpayCommand());

        assertEquals(existingPaymentId, result.paymentId());
        assertEquals("PENDING", result.status());
        assertEquals("https://sandbox.vnpay.vn/old-payment-url", result.paymentUrl());
        assertEquals(0, paymentRepository.savedPayments.size());
    }

    @Test
    void shouldRejectSameIdempotencyKeyWithDifferentRequestHash() {
        FakePaymentRepositoryPort paymentRepository = new FakePaymentRepositoryPort();
        FakeIdempotencyPort idempotencyPort = new FakeIdempotencyPort();
        FakeOutboxPort outboxPort = new FakeOutboxPort();

        idempotencyPort.record = new IdempotencyRecord(
                IdempotencyScope.payment(new CheckoutGroupId(vnpayCommand().checkoutGroupId())),
                "idem-key-1",
                "different-hash",
                IdempotencyStatus.SUCCEEDED,
                UUID.randomUUID() + "|PENDING|https://sandbox.vnpay.vn/old-payment-url",
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );

        CreatePaymentService service = newService(
                paymentRepository,
                idempotencyPort,
                outboxPort,
                new FixedIdGeneratorPort(UUID.randomUUID()),
                new FakeVnpayGatewayPort()
        );

        assertThrows(
                IdempotencyKeyReuseException.class,
                () -> service.execute(vnpayCommand())
        );
    }

    @Test
    void shouldRejectProcessingIdempotencyRequest() {
        FakePaymentRepositoryPort paymentRepository = new FakePaymentRepositoryPort();
        FakeIdempotencyPort idempotencyPort = new FakeIdempotencyPort();
        FakeOutboxPort outboxPort = new FakeOutboxPort();

        idempotencyPort.record = new IdempotencyRecord(
                IdempotencyScope.payment(new CheckoutGroupId(vnpayCommand().checkoutGroupId())),
                "idem-key-1",
                "request-hash",
                IdempotencyStatus.PROCESSING,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );

        CreatePaymentService service = newService(
                paymentRepository,
                idempotencyPort,
                outboxPort,
                new FixedIdGeneratorPort(UUID.randomUUID()),
                new FakeVnpayGatewayPort()
        );

        assertThrows(
                RequestAlreadyProcessingException.class,
                () -> service.execute(vnpayCommand())
        );
    }

    @Test
    void shouldRejectUnsupportedPaymentMethod() {
        CreatePaymentOrderCommand order = new CreatePaymentOrderCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                100_000
        );

        CreatePaymentCommand command = new CreatePaymentCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "MOMO",
                100_000,
                "VND",
                "idem-key-1",
                List.of(order),
                "127.0.0.1"
        );

        CreatePaymentService service = newService(
                new FakePaymentRepositoryPort(),
                new FakeIdempotencyPort(),
                new FakeOutboxPort(),
                new FixedIdGeneratorPort(UUID.randomUUID()),
                new FakeVnpayGatewayPort()
        );

        assertThrows(
                UnsupportedPaymentMethodException.class,
                () -> service.execute(command)
        );
    }

    private CreatePaymentService newService(
            FakePaymentRepositoryPort paymentRepository,
            FakeIdempotencyPort idempotencyPort,
            FakeOutboxPort outboxPort,
            IdGeneratorPort idGeneratorPort,
            VnpayGatewayPort vnpayGatewayPort
    ) {
        return new CreatePaymentService(
                paymentRepository,
                idempotencyPort,
                new FakeRequestHashPort(),
                idGeneratorPort,
                new FixedClockPort(),
                vnpayGatewayPort,
                outboxPort,
                new ImmediateTransactionPort(),
                new PipeSeparatedCreatePaymentResultPayloadPort()
        );
    }

    private CreatePaymentCommand vnpayCommand() {
        CreatePaymentOrderCommand order = new CreatePaymentOrderCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                100_000
        );

        return new CreatePaymentCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "VNPAY",
                100_000,
                "VND",
                "idem-key-1",
                List.of(order),
                "127.0.0.1"
        );
    }

    private CreatePaymentCommand codCommand() {
        CreatePaymentOrderCommand order = new CreatePaymentOrderCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                100_000
        );

        return new CreatePaymentCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "COD",
                100_000,
                "VND",
                "idem-key-1",
                List.of(order),
                null
        );
    }

    private static final class FakePaymentRepositoryPort implements PaymentRepositoryPort {

        private final List<Payment> savedPayments = new ArrayList<>();

        @Override
        public Optional<Payment> findById(PaymentId paymentId) {
            return Optional.empty();
        }

        @Override
        public Optional<Payment> findByCheckoutGroupId(CheckoutGroupId checkoutGroupId) {
            return Optional.empty();
        }

        @Override
        public Payment save(Payment payment) {
            savedPayments.add(payment);
            return payment;
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
            return "request-hash";
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

    private static final class FixedClockPort implements ClockPort {

        @Override
        public Instant now() {
            return Instant.parse("2026-01-01T00:00:00Z");
        }
    }

    private static final class FakeVnpayGatewayPort implements VnpayGatewayPort {

        @Override
        public CreateVnpayPaymentUrlResult createPaymentUrl(
                CreateVnpayPaymentUrlRequest request
        ) {
            return new CreateVnpayPaymentUrlResult(
                    "VNPAY-TXN-001",
                    "https://sandbox.vnpay.vn/payment-url",
                    request.expiresAt()
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

    private static final class ImmediateTransactionPort implements TransactionPort {

        @Override
        public <T> T execute(Supplier<T> action) {
            return action.get();
        }
    }

    private static final class FixedIdGeneratorPort implements IdGeneratorPort {

        private final UUID paymentUuid;

        private FixedIdGeneratorPort(UUID paymentUuid) {
            this.paymentUuid = paymentUuid;
        }

        @Override
        public PaymentId nextPaymentId() {
            return new PaymentId(paymentUuid);
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

    private static final class PipeSeparatedCreatePaymentResultPayloadPort
            implements CreatePaymentResultPayloadPort {

        @Override
        public String serialize(CreatePaymentResult result) {
            return result.paymentId()
                    + "|"
                    + result.status()
                    + "|"
                    + result.paymentUrl();
        }

        @Override
        public CreatePaymentResult deserialize(String payload) {
            String[] parts = payload.split("\\|", -1);

            String paymentUrl = "null".equals(parts[2]) ? null : parts[2];

            return new CreatePaymentResult(
                    UUID.fromString(parts[0]),
                    parts[1],
                    paymentUrl
            );
        }
    }
}
