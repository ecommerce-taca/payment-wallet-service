package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.ProcessVnpayWebhookCommand;
import com.taca.paymentwallet.application.exception.PaymentAmountMismatchException;
import com.taca.paymentwallet.application.exception.PaymentNotFoundException;
import com.taca.paymentwallet.application.fee.PaymentFeePolicy;
import com.taca.paymentwallet.application.paymentevent.PaymentProviderEvent;
import com.taca.paymentwallet.application.paymentevent.PaymentProviderEventStatus;
import com.taca.paymentwallet.application.port.in.ProcessVnpayWebhookUseCase;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.FeePolicyPort;
import com.taca.paymentwallet.application.port.out.IdGeneratorPort;
import com.taca.paymentwallet.application.port.out.LedgerAccountLookupPort;
import com.taca.paymentwallet.application.port.out.LedgerPostingRepositoryPort;
import com.taca.paymentwallet.application.port.out.OutboxPort;
import com.taca.paymentwallet.application.port.out.PaymentAllocationRepositoryPort;
import com.taca.paymentwallet.application.port.out.PaymentProviderEventPort;
import com.taca.paymentwallet.application.port.out.PaymentRepositoryPort;
import com.taca.paymentwallet.application.port.out.TransactionPort;
import com.taca.paymentwallet.application.port.out.VnpayWebhookVerifierPort;
import com.taca.paymentwallet.application.result.ProcessVnpayWebhookResult;
import com.taca.paymentwallet.application.result.WebhookProcessingAction;
import com.taca.paymentwallet.domain.finance.AllocationCalculator;
import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.wallet.LedgerPosting;
import com.taca.paymentwallet.domain.wallet.LedgerPostingFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProcessVnpayWebhookService implements ProcessVnpayWebhookUseCase {

    private static final String PROVIDER = "VNPAY";
    private static final String VNPAY_SUCCESS_CODE = "00";

    private final PaymentRepositoryPort paymentRepository;
    private final PaymentProviderEventPort paymentProviderEventPort;
    private final PaymentAllocationRepositoryPort paymentAllocationRepository;
    private final LedgerPostingRepositoryPort ledgerPostingRepository;
    private final LedgerAccountLookupPort ledgerAccountLookupPort;
    private final FeePolicyPort feePolicyPort;
    private final VnpayWebhookVerifierPort vnpayWebhookVerifierPort;
    private final IdGeneratorPort idGeneratorPort;
    private final ClockPort clockPort;
    private final OutboxPort outboxPort;
    private final TransactionPort transactionPort;
    private final AllocationCalculator allocationCalculator;
    private final LedgerPostingFactory ledgerPostingFactory;

    public ProcessVnpayWebhookService(
            PaymentRepositoryPort paymentRepository,
            PaymentProviderEventPort paymentProviderEventPort,
            PaymentAllocationRepositoryPort paymentAllocationRepository,
            LedgerPostingRepositoryPort ledgerPostingRepository,
            LedgerAccountLookupPort ledgerAccountLookupPort,
            FeePolicyPort feePolicyPort,
            VnpayWebhookVerifierPort vnpayWebhookVerifierPort,
            IdGeneratorPort idGeneratorPort,
            ClockPort clockPort,
            OutboxPort outboxPort,
            TransactionPort transactionPort,
            AllocationCalculator allocationCalculator,
            LedgerPostingFactory ledgerPostingFactory
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository);
        this.paymentProviderEventPort = Objects.requireNonNull(paymentProviderEventPort);
        this.paymentAllocationRepository = Objects.requireNonNull(paymentAllocationRepository);
        this.ledgerPostingRepository = Objects.requireNonNull(ledgerPostingRepository);
        this.ledgerAccountLookupPort = Objects.requireNonNull(ledgerAccountLookupPort);
        this.feePolicyPort = Objects.requireNonNull(feePolicyPort);
        this.vnpayWebhookVerifierPort = Objects.requireNonNull(vnpayWebhookVerifierPort);
        this.idGeneratorPort = Objects.requireNonNull(idGeneratorPort);
        this.clockPort = Objects.requireNonNull(clockPort);
        this.outboxPort = Objects.requireNonNull(outboxPort);
        this.transactionPort = Objects.requireNonNull(transactionPort);
        this.allocationCalculator = Objects.requireNonNull(allocationCalculator);
        this.ledgerPostingFactory = Objects.requireNonNull(ledgerPostingFactory);
    }

    @Override
    public ProcessVnpayWebhookResult execute(ProcessVnpayWebhookCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        vnpayWebhookVerifierPort.verify(command);

        return transactionPort.execute(() -> process(command));
    }

    private ProcessVnpayWebhookResult process(ProcessVnpayWebhookCommand command) {
        PaymentId paymentId = new PaymentId(command.paymentId());
        Money webhookAmount = new Money(command.amount(), command.currency());

        boolean inserted = paymentProviderEventPort.recordIfAbsent(
                new PaymentProviderEvent(
                        PROVIDER,
                        command.providerEventId(),
                        command.providerTransactionRef(),
                        paymentId,
                        command.responseCode(),
                        command.transactionStatus(),
                        webhookAmount,
                        command.payloadHash(),
                        clockPort.now(),
                        PaymentProviderEventStatus.RECEIVED
                )
        );

        if (!inserted) {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException(paymentId));

            return new ProcessVnpayWebhookResult(
                    payment.id().value(),
                    payment.status().name(),
                    WebhookProcessingAction.DUPLICATE
            );
        }

        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        ensureAmountMatches(payment, webhookAmount);

        if (isVnpaySuccess(command)) {
            applySuccessfulPayment(payment);
        } else {
            payment.markFailed(vnpayFailureCode(command));
            paymentRepository.save(payment);
            outboxPort.saveAll(payment.domainEvents());
            payment.clearDomainEvents();
        }

        paymentProviderEventPort.markApplied(PROVIDER, command.providerEventId());

        return new ProcessVnpayWebhookResult(
                payment.id().value(),
                payment.status().name(),
                WebhookProcessingAction.APPLIED
        );
    }

    private void applySuccessfulPayment(Payment payment) {
        payment.markSucceeded(clockPort.now());

        PaymentFeePolicy feePolicy = feePolicyPort.currentPaymentFeePolicy();

        List<PaymentAllocation> allocations = allocationCalculator.allocate(
                payment.orders(),
                allocationIdsByOrder(payment.orders()),
                feePolicy.commissionRate(),
                feePolicy.taxRate()
        );

        LedgerPosting posting = ledgerPostingFactory.createPaymentCapturePosting(
                idGeneratorPort.nextLedgerPostingId(),
                payment.id(),
                ledgerAccountLookupPort.vnpayClearingAccount(),
                ledgerAccountLookupPort.platformCommissionAccount(),
                ledgerAccountLookupPort.taxPayableAccount(),
                ledgerAccountLookupPort.sellerPendingAccountsFor(distinctShopIds(payment.orders())),
                allocations
        );

        paymentRepository.save(payment);
        paymentAllocationRepository.saveAll(allocations);
        ledgerPostingRepository.save(posting);
        outboxPort.saveAll(payment.domainEvents());
        payment.clearDomainEvents();
    }

    private Map<OrderId, PaymentAllocationId> allocationIdsByOrder(List<PaymentOrder> orders) {
        Map<OrderId, PaymentAllocationId> result = new LinkedHashMap<>();

        for (PaymentOrder order : orders) {
            result.put(order.orderId(), idGeneratorPort.nextPaymentAllocationId());
        }

        return result;
    }

    private List<ShopId> distinctShopIds(List<PaymentOrder> orders) {
        return orders.stream()
                .map(PaymentOrder::shopId)
                .distinct()
                .toList();
    }

    private void ensureAmountMatches(Payment payment, Money webhookAmount) {
        if (!payment.amount().equals(webhookAmount)) {
            throw new PaymentAmountMismatchException(
                    payment.id(),
                    payment.amount(),
                    webhookAmount
            );
        }
    }

    private boolean isVnpaySuccess(ProcessVnpayWebhookCommand command) {
        return VNPAY_SUCCESS_CODE.equals(command.responseCode())
                && VNPAY_SUCCESS_CODE.equals(command.transactionStatus());
    }

    private String vnpayFailureCode(ProcessVnpayWebhookCommand command) {
        return "VNPAY_" + command.responseCode() + "_" + command.transactionStatus();
    }
}