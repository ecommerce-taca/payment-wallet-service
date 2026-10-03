package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.ProcessCodPaymentCommand;
import com.taca.paymentwallet.application.exception.*;
import com.taca.paymentwallet.application.fee.PaymentFeePolicy;
import com.taca.paymentwallet.application.payment.CodPaymentProcessingAction;
import com.taca.paymentwallet.application.payment.CodPaymentResultStatus;
import com.taca.paymentwallet.application.port.in.ProcessCodPaymentUseCase;
import com.taca.paymentwallet.application.port.out.FeePolicyPort;
import com.taca.paymentwallet.application.port.out.IdGeneratorPort;
import com.taca.paymentwallet.application.port.out.LedgerAccountLookupPort;
import com.taca.paymentwallet.application.port.out.LedgerPostingRepositoryPort;
import com.taca.paymentwallet.application.port.out.OutboxPort;
import com.taca.paymentwallet.application.port.out.PaymentAllocationRepositoryPort;
import com.taca.paymentwallet.application.port.out.PaymentRepositoryPort;
import com.taca.paymentwallet.application.port.out.TransactionPort;
import com.taca.paymentwallet.application.port.out.WalletRepositoryPort;
import com.taca.paymentwallet.application.result.ProcessCodPaymentResult;
import com.taca.paymentwallet.domain.finance.AllocationCalculator;
import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.payment.PaymentMethod;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.valueobject.*;
import com.taca.paymentwallet.domain.wallet.LedgerPosting;
import com.taca.paymentwallet.domain.wallet.LedgerPostingFactory;
import com.taca.paymentwallet.domain.wallet.Wallet;
import com.taca.paymentwallet.domain.wallet.WalletAllocatedEvent;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProcessCodPaymentService implements ProcessCodPaymentUseCase {

    private final PaymentRepositoryPort paymentRepository;
    private final PaymentAllocationRepositoryPort paymentAllocationRepository;
    private final WalletRepositoryPort walletRepository;
    private final LedgerPostingRepositoryPort ledgerPostingRepository;
    private final LedgerAccountLookupPort ledgerAccountLookupPort;
    private final FeePolicyPort feePolicyPort;
    private final IdGeneratorPort idGeneratorPort;
    private final OutboxPort outboxPort;
    private final TransactionPort transactionPort;
    private final AllocationCalculator allocationCalculator;
    private final LedgerPostingFactory ledgerPostingFactory;

    public ProcessCodPaymentService(
            PaymentRepositoryPort paymentRepository,
            PaymentAllocationRepositoryPort paymentAllocationRepository,
            WalletRepositoryPort walletRepository,
            LedgerPostingRepositoryPort ledgerPostingRepository,
            LedgerAccountLookupPort ledgerAccountLookupPort,
            FeePolicyPort feePolicyPort,
            IdGeneratorPort idGeneratorPort,
            OutboxPort outboxPort,
            TransactionPort transactionPort,
            AllocationCalculator allocationCalculator,
            LedgerPostingFactory ledgerPostingFactory
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository);
        this.paymentAllocationRepository = Objects.requireNonNull(paymentAllocationRepository);
        this.walletRepository = Objects.requireNonNull(walletRepository);
        this.ledgerPostingRepository = Objects.requireNonNull(ledgerPostingRepository);
        this.ledgerAccountLookupPort = Objects.requireNonNull(ledgerAccountLookupPort);
        this.feePolicyPort = Objects.requireNonNull(feePolicyPort);
        this.idGeneratorPort = Objects.requireNonNull(idGeneratorPort);
        this.outboxPort = Objects.requireNonNull(outboxPort);
        this.transactionPort = Objects.requireNonNull(transactionPort);
        this.allocationCalculator = Objects.requireNonNull(allocationCalculator);
        this.ledgerPostingFactory = Objects.requireNonNull(ledgerPostingFactory);
    }

    @Override
    public ProcessCodPaymentResult execute(ProcessCodPaymentCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return transactionPort.execute(() -> process(command));
    }

    private ProcessCodPaymentResult process(ProcessCodPaymentCommand command) {
        OrderId orderId = new OrderId(command.orderId());

        Payment payment = paymentRepository
                .findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new PaymentNotFoundByOrderException(orderId));

        ensureCodPayment(payment);

        PaymentOrder order = payment.order(orderId);

        if (!order.isCodPending()) {
            return toResult(payment, order, CodPaymentProcessingAction.DUPLICATE);
        }

        if (command.status() == CodPaymentResultStatus.DELIVERED) {
            return applyDeliveredCodOrder(payment, order, command);
        }

        return applyFailedCodOrder(payment, order, command);
    }

    private ProcessCodPaymentResult applyDeliveredCodOrder(
            Payment payment,
            PaymentOrder order,
            ProcessCodPaymentCommand command
    ) {
        Wallet wallet = walletRepository
                .findByShopIdAndCurrencyForUpdate(
                        order.shopId(),
                        order.totalAmount().currency()
                )
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                order.shopId(),
                                order.totalAmount().currency()
                        ));

        PaymentFeePolicy feePolicy = feePolicyPort.currentPaymentFeePolicy();

        PaymentOrder capturedOrder =
                payment.captureCodOrder(order.orderId(), command.occurredAt());

        PaymentAllocation allocation = allocationCalculator.allocate(
                payment.id(),
                List.of(capturedOrder),
                Map.of(
                        capturedOrder.orderId(),
                        idGeneratorPort.nextPaymentAllocationId()
                ),
                Map.of(capturedOrder.shopId(), wallet.id()),
                feePolicy.feeConfigId(),
                feePolicy.taxConfigId(),
                feePolicy.commissionRate(),
                feePolicy.taxRate()
        ).getFirst();

        LedgerPosting posting =
                ledgerPostingFactory.createCodOrderCapturePosting(
                        idGeneratorPort.nextLedgerPostingId(),
                        payment.id(),
                        capturedOrder.orderId(),
                        ledgerAccountLookupPort.codClearingAccount(),
                        ledgerAccountLookupPort.platformCommissionAccount(),
                        ledgerAccountLookupPort.taxPayableAccount(),
                        ledgerAccountLookupPort.shipmentPayableAccount(),
                        ledgerAccountLookupPort.sellerPendingAccountsFor(
                                List.of(capturedOrder.shopId())
                        ),
                        allocation,
                        capturedOrder.shippingFee()
                );

        wallet.creditPending(allocation.sellerNetAmount());

        paymentRepository.save(payment);
        paymentAllocationRepository.saveAll(List.of(allocation));
        ledgerPostingRepository.save(posting);
        walletRepository.save(wallet);

        outboxPort.saveAll(payment.domainEvents());
        outboxPort.save(WalletAllocatedEvent.from(allocation, command.occurredAt()));
        payment.clearDomainEvents();

        return toResult(payment, capturedOrder, CodPaymentProcessingAction.APPLIED);
    }

    private ProcessCodPaymentResult applyFailedCodOrder(
            Payment payment,
            PaymentOrder order,
            ProcessCodPaymentCommand command
    ) {
        PaymentOrder failedOrder =
                payment.failCodOrder(
                        order.orderId(),
                        command.occurredAt(),
                        command.failureCode()
                );

        paymentRepository.save(payment);
        outboxPort.saveAll(payment.domainEvents());
        payment.clearDomainEvents();

        return toResult(payment, failedOrder, CodPaymentProcessingAction.APPLIED);
    }

    private void ensureCodPayment(Payment payment) {
        if (payment.method() != PaymentMethod.COD) {
            throw new UnsupportedPaymentMethodException(payment.method().name());
        }
    }

    private ProcessCodPaymentResult toResult(
            Payment payment,
            PaymentOrder order,
            CodPaymentProcessingAction action
    ) {
        return new ProcessCodPaymentResult(
                payment.id().value(),
                payment.checkoutGroupId().value(),
                order.orderId().value(),
                payment.status().name(),
                order.codStatus().name(),
                action
        );
    }
}
