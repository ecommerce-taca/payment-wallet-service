package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.CreatePaymentCommand;
import com.taca.paymentwallet.application.command.CreatePaymentOrderCommand;
import com.taca.paymentwallet.application.exception.IdempotencyKeyReuseException;
import com.taca.paymentwallet.application.exception.RequestAlreadyProcessingException;
import com.taca.paymentwallet.application.exception.UnsupportedPaymentMethodException;
import com.taca.paymentwallet.application.gateway.vnpay.CreateVnpayPaymentUrlRequest;
import com.taca.paymentwallet.application.gateway.vnpay.CreateVnpayPaymentUrlResult;
import com.taca.paymentwallet.application.idempotency.IdempotencyRecord;
import com.taca.paymentwallet.application.idempotency.IdempotencyScope;
import com.taca.paymentwallet.application.port.in.CreatePaymentUseCase;
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
import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.payment.PaymentMethod;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.valueobject.BuyerUserId;
import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.ShopId;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class CreatePaymentService implements CreatePaymentUseCase {

    private static final Duration VNPAY_PAYMENT_TTL = Duration.ofMinutes(15);

    private final PaymentRepositoryPort paymentRepository;
    private final IdempotencyPort idempotencyPort;
    private final RequestHashPort requestHashPort;
    private final IdGeneratorPort idGeneratorPort;
    private final ClockPort clockPort;
    private final VnpayGatewayPort vnpayGatewayPort;
    private final OutboxPort outboxPort;
    private final TransactionPort transactionPort;
    private final CreatePaymentResultPayloadPort resultPayloadPort;

    public CreatePaymentService(
            PaymentRepositoryPort paymentRepository,
            IdempotencyPort idempotencyPort,
            RequestHashPort requestHashPort,
            IdGeneratorPort idGeneratorPort,
            ClockPort clockPort,
            VnpayGatewayPort vnpayGatewayPort,
            OutboxPort outboxPort,
            TransactionPort transactionPort,
            CreatePaymentResultPayloadPort resultPayloadPort
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository);
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort);
        this.requestHashPort = Objects.requireNonNull(requestHashPort);
        this.idGeneratorPort = Objects.requireNonNull(idGeneratorPort);
        this.clockPort = Objects.requireNonNull(clockPort);
        this.vnpayGatewayPort = Objects.requireNonNull(vnpayGatewayPort);
        this.outboxPort = Objects.requireNonNull(outboxPort);
        this.transactionPort = Objects.requireNonNull(transactionPort);
        this.resultPayloadPort = Objects.requireNonNull(resultPayloadPort);
    }

    @Override
    public CreatePaymentResult execute(CreatePaymentCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        String requestHash = requestHashPort.hash(command);
        CheckoutGroupId checkoutGroupId = new CheckoutGroupId(command.checkoutGroupId());
        IdempotencyScope scope = IdempotencyScope.payment(checkoutGroupId);

        return transactionPort.execute(() -> {
            CreatePaymentResult existingResult = resolveExistingIdempotencyResult(
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

            CreatePaymentResult result = createNewPayment(command, checkoutGroupId);

            idempotencyPort.markSucceeded(
                    scope,
                    command.idempotencyKey(),
                    resultPayloadPort.serialize(result)
            );

            return result;
        });
    }

    private CreatePaymentResult resolveExistingIdempotencyResult(
            IdempotencyScope scope,
            String idempotencyKey,
            String requestHash
    ) {
        return idempotencyPort.find(scope, idempotencyKey)
                .map(record -> resolveExistingRecord(record, idempotencyKey, requestHash))
                .orElse(null);
    }

    private CreatePaymentResult resolveExistingRecord(
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

    private CreatePaymentResult createNewPayment(
            CreatePaymentCommand command,
            CheckoutGroupId checkoutGroupId
    ) {
        PaymentMethod method = parsePaymentMethod(command.method());
        PaymentId paymentId = idGeneratorPort.nextPaymentId();

        Payment payment = Payment.create(
                paymentId,
                checkoutGroupId,
                new BuyerUserId(command.buyerUserId()),
                method,
                new Money(command.amount(), command.currency()),
                toDomainOrders(command.orders())
        );

        CreatePaymentResult result = switch (method) {
            case VNPAY -> createVnpayPayment(command, payment);
            case COD -> createCodPayment(payment);
        };

        paymentRepository.save(payment);
        outboxPort.saveAll(payment.domainEvents());
        payment.clearDomainEvents();

        return result;
    }

    private CreatePaymentResult createVnpayPayment(
            CreatePaymentCommand command,
            Payment payment
    ) {
        Instant expiresAt = clockPort.now().plus(VNPAY_PAYMENT_TTL);

        CreateVnpayPaymentUrlResult vnpayResult = vnpayGatewayPort.createPaymentUrl(
                new CreateVnpayPaymentUrlRequest(
                        payment.id(),
                        payment.checkoutGroupId(),
                        payment.amount(),
                        expiresAt,
                        requireClientIp(command.clientIp())
                )
        );

        return new CreatePaymentResult(
                payment.id().value(),
                payment.status().name(),
                vnpayResult.paymentUrl()
        );
    }

    private CreatePaymentResult createCodPayment(Payment payment) {
        return new CreatePaymentResult(
                payment.id().value(),
                payment.status().name(),
                null
        );
    }

    private List<PaymentOrder> toDomainOrders(List<CreatePaymentOrderCommand> orders) {
        return orders.stream()
                .map(order -> new PaymentOrder(
                        new OrderId(order.orderId()),
                        new ShopId(order.shopId()),
                        Money.vnd(order.amount())
                ))
                .toList();
    }

    private PaymentMethod parsePaymentMethod(String method) {
        try {
            return PaymentMethod.valueOf(method);
        } catch (IllegalArgumentException exception) {
            throw new UnsupportedPaymentMethodException(method);
        }
    }

    private String requireClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            throw new IllegalArgumentException("clientIp must not be blank for VNPAY payment");
        }

        return clientIp.trim();
    }
}
