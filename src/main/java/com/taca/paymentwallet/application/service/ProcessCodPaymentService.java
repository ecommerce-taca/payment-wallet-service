package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.ProcessCodPaymentCommand;
import com.taca.paymentwallet.application.exception.PaymentAmountMismatchException;
import com.taca.paymentwallet.application.exception.PaymentNotFoundByCheckoutGroupException;
import com.taca.paymentwallet.application.exception.UnsupportedPaymentMethodException;
import com.taca.paymentwallet.application.exception.WalletNotFoundException;
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
import com.taca.paymentwallet.domain.payment.PaymentStatus;
import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.wallet.LedgerPosting;
import com.taca.paymentwallet.domain.wallet.LedgerPostingFactory;
import com.taca.paymentwallet.domain.wallet.Wallet;

import java.util.LinkedHashMap;
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
        CheckoutGroupId checkoutGroupId =
                new CheckoutGroupId(command.checkoutGroupId());

        Payment payment = paymentRepository
                .findByCheckoutGroupIdForUpdate(checkoutGroupId)
                .orElseThrow(() -> new PaymentNotFoundByCheckoutGroupException(checkoutGroupId));

        ensureCodPayment(payment);

        if (isTerminal(payment)) {
            return toResult(payment, CodPaymentProcessingAction.DUPLICATE);
        }

        ensureAmountMatches(payment, new Money(command.amount(), command.currency()));

        if (command.status() == CodPaymentResultStatus.DELIVERED) {
            applyDeliveredCodPayment(payment, command);
        } else {
            payment.markFailed(command.failureCode());
            paymentRepository.save(payment);
            outboxPort.saveAll(payment.domainEvents());
            payment.clearDomainEvents();
        }

        return toResult(payment, CodPaymentProcessingAction.APPLIED);
    }

    private void applyDeliveredCodPayment(
            Payment payment,
            ProcessCodPaymentCommand command
    ) {
        payment.markSucceeded(command.occurredAt());

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
                ledgerAccountLookupPort.codClearingAccount(),
                ledgerAccountLookupPort.platformCommissionAccount(),
                ledgerAccountLookupPort.taxPayableAccount(),
                ledgerAccountLookupPort.sellerPendingAccountsFor(distinctShopIds(payment.orders())),
                allocations
        );

        creditSellerPendingWallets(allocations);

        paymentRepository.save(payment);
        paymentAllocationRepository.saveAll(allocations);
        ledgerPostingRepository.save(posting);

        outboxPort.saveAll(payment.domainEvents());
        payment.clearDomainEvents();
    }

    private void creditSellerPendingWallets(List<PaymentAllocation> allocations) {
        Map<ShopId, Money> sellerNetAmountByShop = new LinkedHashMap<>();

        for (PaymentAllocation allocation : allocations) {
            sellerNetAmountByShop.merge(
                    allocation.shopId(),
                    allocation.sellerNetAmount(),
                    Money::add
            );
        }

        for (Map.Entry<ShopId, Money> entry : sellerNetAmountByShop.entrySet()) {
            ShopId shopId = entry.getKey();
            Money sellerNetAmount = entry.getValue();

            Wallet wallet = walletRepository
                    .findByShopIdAndCurrencyForUpdate(shopId, sellerNetAmount.currency())
                    .orElseThrow(() -> new WalletNotFoundException(
                            shopId,
                            sellerNetAmount.currency()
                    ));

            wallet.creditPending(sellerNetAmount);
            walletRepository.save(wallet);
        }
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

    private void ensureCodPayment(Payment payment) {
        if (payment.method() != PaymentMethod.COD) {
            throw new UnsupportedPaymentMethodException(payment.method().name());
        }
    }

    private boolean isTerminal(Payment payment) {
        return payment.status() == PaymentStatus.SUCCESS
                || payment.status() == PaymentStatus.FAILED
                || payment.status() == PaymentStatus.EXPIRED
                || payment.status() == PaymentStatus.PARTIALLY_REFUNDED
                || payment.status() == PaymentStatus.REFUNDED;
    }

    private void ensureAmountMatches(Payment payment, Money actualAmount) {
        if (!payment.amount().equals(actualAmount)) {
            throw new PaymentAmountMismatchException(
                    payment.id(),
                    payment.amount(),
                    actualAmount
            );
        }
    }

    private ProcessCodPaymentResult toResult(
            Payment payment,
            CodPaymentProcessingAction action
    ) {
        return new ProcessCodPaymentResult(
                payment.id().value(),
                payment.checkoutGroupId().value(),
                payment.status().name(),
                action
        );
    }
}
