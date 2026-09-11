package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.ProcessRefundResultCommand;
import com.taca.paymentwallet.application.exception.PaymentNotFoundException;
import com.taca.paymentwallet.application.exception.RefundAmountMismatchException;
import com.taca.paymentwallet.application.exception.RefundNotFoundException;
import com.taca.paymentwallet.application.port.in.ProcessRefundResultUseCase;
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
import com.taca.paymentwallet.domain.finance.RefundAllocationCalculator;
import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.refund.Refund;
import com.taca.paymentwallet.domain.refund.RefundAllocation;
import com.taca.paymentwallet.domain.refund.RefundStatus;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.RefundId;
import com.taca.paymentwallet.domain.wallet.LedgerPosting;
import com.taca.paymentwallet.domain.wallet.LedgerPostingFactory;

import java.util.List;
import java.util.Objects;

public class ProcessRefundResultService implements ProcessRefundResultUseCase {

    private final RefundRepositoryPort refundRepository;
    private final PaymentRepositoryPort paymentRepository;
    private final PaymentAllocationRepositoryPort paymentAllocationRepository;
    private final RefundAllocationRepositoryPort refundAllocationRepository;
    private final LedgerPostingRepositoryPort ledgerPostingRepository;
    private final LedgerAccountLookupPort ledgerAccountLookupPort;
    private final OutboxPort outboxPort;
    private final TransactionPort transactionPort;
    private final com.taca.paymentwallet.application.port.out.IdGeneratorPort idGeneratorPort;
    private final RefundAllocationCalculator refundAllocationCalculator;
    private final LedgerPostingFactory ledgerPostingFactory;

    public ProcessRefundResultService(
            RefundRepositoryPort refundRepository,
            PaymentRepositoryPort paymentRepository,
            PaymentAllocationRepositoryPort paymentAllocationRepository,
            RefundAllocationRepositoryPort refundAllocationRepository,
            LedgerPostingRepositoryPort ledgerPostingRepository,
            LedgerAccountLookupPort ledgerAccountLookupPort,
            OutboxPort outboxPort,
            TransactionPort transactionPort,
            com.taca.paymentwallet.application.port.out.IdGeneratorPort idGeneratorPort,
            RefundAllocationCalculator refundAllocationCalculator,
            LedgerPostingFactory ledgerPostingFactory
    ) {
        this.refundRepository = Objects.requireNonNull(refundRepository);
        this.paymentRepository = Objects.requireNonNull(paymentRepository);
        this.paymentAllocationRepository = Objects.requireNonNull(paymentAllocationRepository);
        this.refundAllocationRepository = Objects.requireNonNull(refundAllocationRepository);
        this.ledgerPostingRepository = Objects.requireNonNull(ledgerPostingRepository);
        this.ledgerAccountLookupPort = Objects.requireNonNull(ledgerAccountLookupPort);
        this.outboxPort = Objects.requireNonNull(outboxPort);
        this.transactionPort = Objects.requireNonNull(transactionPort);
        this.idGeneratorPort = Objects.requireNonNull(idGeneratorPort);
        this.refundAllocationCalculator = Objects.requireNonNull(refundAllocationCalculator);
        this.ledgerPostingFactory = Objects.requireNonNull(ledgerPostingFactory);
    }

    @Override
    public ProcessRefundResult execute(ProcessRefundResultCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return transactionPort.execute(() -> process(command));
    }

    private ProcessRefundResult process(ProcessRefundResultCommand command) {
        RefundId refundId = new RefundId(command.refundId());

        Refund refund = refundRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new RefundNotFoundException(refundId));

        Payment payment = paymentRepository.findByIdForUpdate(refund.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(refund.paymentId()));

        if (isTerminal(refund)) {
            return toResult(refund, payment, RefundResultProcessingAction.DUPLICATE);
        }

        ensureAmountMatches(refund, new Money(command.amount(), command.currency()));

        if (command.status() == RefundResultStatus.FAILED) {
            refund.markFailed(command.failureCode());
            refundRepository.save(refund);
            outboxPort.saveAll(refund.domainEvents());
            refund.clearDomainEvents();

            return toResult(refund, payment, RefundResultProcessingAction.APPLIED);
        }

        applySuccessfulRefund(refund, payment);

        return toResult(refund, payment, RefundResultProcessingAction.APPLIED);
    }

    private void applySuccessfulRefund(Refund refund, Payment payment) {
        List<PaymentAllocation> paymentAllocations =
                paymentAllocationRepository.findByPaymentId(payment.id());

        List<RefundAllocation> refundAllocations =
                refundAllocationCalculator.allocate(refund.amount(), paymentAllocations);

        List<PaymentAllocationId> allocationIds = refundAllocations.stream()
                .map(RefundAllocation::paymentAllocationId)
                .toList();

        LedgerPosting posting = ledgerPostingFactory.createRefundSuccessPosting(
                idGeneratorPort.nextLedgerPostingId(),
                refund.id(),
                ledgerAccountLookupPort.refundClearingAccount(),
                ledgerAccountLookupPort.platformCommissionAccount(),
                ledgerAccountLookupPort.taxPayableAccount(),
                ledgerAccountLookupPort.sellerRefundAccountsFor(allocationIds),
                refundAllocations
        );

        refund.markSucceeded();
        payment.markRefundSucceeded(refund.amount());

        paymentRepository.save(payment);
        refundRepository.save(refund);
        refundAllocationRepository.saveAll(refundAllocations);
        ledgerPostingRepository.save(posting);

        outboxPort.saveAll(refund.domainEvents());
        outboxPort.saveAll(payment.domainEvents());

        refund.clearDomainEvents();
        payment.clearDomainEvents();
    }

    private boolean isTerminal(Refund refund) {
        return refund.status() == RefundStatus.SUCCESS
                || refund.status() == RefundStatus.FAILED
                || refund.status() == RefundStatus.CANCELLED;
    }

    private void ensureAmountMatches(Refund refund, Money actual) {
        if (!refund.amount().equals(actual)) {
            throw new RefundAmountMismatchException(
                    refund.id(),
                    refund.amount(),
                    actual
            );
        }
    }

    private ProcessRefundResult toResult(
            Refund refund,
            Payment payment,
            RefundResultProcessingAction action
    ) {
        return new ProcessRefundResult(
                refund.id().value(),
                payment.id().value(),
                refund.status().name(),
                payment.status().name(),
                action
        );
    }
}