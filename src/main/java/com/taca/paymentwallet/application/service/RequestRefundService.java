package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.RequestRefundCommand;
import com.taca.paymentwallet.application.exception.IdempotencyKeyReuseException;
import com.taca.paymentwallet.application.exception.PaymentNotFoundException;
import com.taca.paymentwallet.application.exception.RequestAlreadyProcessingException;
import com.taca.paymentwallet.application.idempotency.IdempotencyRecord;
import com.taca.paymentwallet.application.idempotency.IdempotencyScope;
import com.taca.paymentwallet.application.port.in.RequestRefundUseCase;
import com.taca.paymentwallet.application.port.out.IdGeneratorPort;
import com.taca.paymentwallet.application.port.out.IdempotencyPort;
import com.taca.paymentwallet.application.port.out.OutboxPort;
import com.taca.paymentwallet.application.port.out.PaymentRepositoryPort;
import com.taca.paymentwallet.application.port.out.RefundRepositoryPort;
import com.taca.paymentwallet.application.port.out.RequestHashPort;
import com.taca.paymentwallet.application.port.out.RequestRefundResultPayloadPort;
import com.taca.paymentwallet.application.port.out.TransactionPort;
import com.taca.paymentwallet.application.result.RequestRefundResult;
import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.refund.Refund;
import com.taca.paymentwallet.domain.valueobject.IdempotencyKey;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.RefundId;

import java.util.Objects;

public class RequestRefundService implements RequestRefundUseCase {

    private final PaymentRepositoryPort paymentRepository;
    private final RefundRepositoryPort refundRepository;
    private final IdempotencyPort idempotencyPort;
    private final RequestHashPort requestHashPort;
    private final IdGeneratorPort idGeneratorPort;
    private final OutboxPort outboxPort;
    private final TransactionPort transactionPort;
    private final RequestRefundResultPayloadPort resultPayloadPort;

    public RequestRefundService(
            PaymentRepositoryPort paymentRepository,
            RefundRepositoryPort refundRepository,
            IdempotencyPort idempotencyPort,
            RequestHashPort requestHashPort,
            IdGeneratorPort idGeneratorPort,
            OutboxPort outboxPort,
            TransactionPort transactionPort,
            RequestRefundResultPayloadPort resultPayloadPort
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository);
        this.refundRepository = Objects.requireNonNull(refundRepository);
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort);
        this.requestHashPort = Objects.requireNonNull(requestHashPort);
        this.idGeneratorPort = Objects.requireNonNull(idGeneratorPort);
        this.outboxPort = Objects.requireNonNull(outboxPort);
        this.transactionPort = Objects.requireNonNull(transactionPort);
        this.resultPayloadPort = Objects.requireNonNull(resultPayloadPort);
    }

    @Override
    public RequestRefundResult execute(RequestRefundCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        PaymentId paymentId = new PaymentId(command.paymentId());
        IdempotencyScope scope = IdempotencyScope.refund(paymentId);
        String requestHash = requestHashPort.hash(command);

        return transactionPort.execute(() -> {
            RequestRefundResult existingResult = resolveExistingIdempotencyResult(
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

            RequestRefundResult result = requestNewRefund(command, paymentId);

            idempotencyPort.markSucceeded(
                    scope,
                    command.idempotencyKey(),
                    resultPayloadPort.serialize(result)
            );

            return result;
        });
    }

    private RequestRefundResult resolveExistingIdempotencyResult(
            IdempotencyScope scope,
            String idempotencyKey,
            String requestHash
    ) {
        return idempotencyPort.find(scope, idempotencyKey)
                .map(record -> resolveExistingRecord(record, idempotencyKey, requestHash))
                .orElse(null);
    }

    private RequestRefundResult resolveExistingRecord(
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

    private RequestRefundResult requestNewRefund(
            RequestRefundCommand command,
            PaymentId paymentId
    ) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        Money refundAmount = new Money(command.amount(), command.currency());
        Money pendingRefundAmount = refundRepository.sumPendingRefundAmount(paymentId);

        payment.validateRefundRequest(refundAmount, pendingRefundAmount);

        RefundId refundId = idGeneratorPort.nextRefundId();

        Refund refund = Refund.request(
                refundId,
                paymentId,
                refundAmount,
                command.reason(),
                new IdempotencyKey(command.idempotencyKey())
        );

        refundRepository.save(refund);
        outboxPort.saveAll(refund.domainEvents());
        refund.clearDomainEvents();

        return new RequestRefundResult(
                refund.id().value(),
                payment.id().value(),
                refund.amount().amount(),
                refund.amount().currency(),
                refund.status().name()
        );
    }
}
