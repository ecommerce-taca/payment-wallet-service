package com.taca.paymentwallet.infrastructure.id;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.IdGeneratorPort;
import com.taca.paymentwallet.domain.valueobject.*;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;

import java.util.Objects;
import java.util.UUID;

public class UuidV7IdGeneratorAdapter
        implements IdGeneratorPort {

    private final ClockPort clockPort;
    private final PersistenceUuidGenerator uuidGenerator;

    public UuidV7IdGeneratorAdapter(
            ClockPort clockPort,
            PersistenceUuidGenerator uuidGenerator
    ) {
        this.clockPort =
                Objects.requireNonNull(clockPort);

        this.uuidGenerator =
                Objects.requireNonNull(uuidGenerator);
    }

    private UUID nextUuid() {
        return uuidGenerator.next(
                clockPort.now()
        );
    }

    @Override
    public PaymentId nextPaymentId() {
        return new PaymentId(nextUuid());
    }

    @Override
    public PaymentAllocationId nextPaymentAllocationId() {
        return new PaymentAllocationId(nextUuid());
    }

    @Override
    public WalletId nextWalletId() {
        return new WalletId(nextUuid());
    }

    @Override
    public LedgerAccountId nextLedgerAccountId() {
        return new LedgerAccountId(nextUuid());
    }

    @Override
    public LedgerPostingId nextLedgerPostingId() {
        return new LedgerPostingId(nextUuid());
    }

    @Override
    public RefundId nextRefundId() {
        return new RefundId(nextUuid());
    }

    @Override
    public PayoutId nextPayoutId() {
        return new PayoutId(nextUuid());
    }

    @Override
    public SettlementBatchId nextSettlementBatchId() {
        return new SettlementBatchId(nextUuid());
    }

    @Override
    public SettlementBatchItemId nextSettlementBatchItemId() {
        return new SettlementBatchItemId(nextUuid());
    }

    @Override
    public SettlementLineId nextSettlementLineId() {
        return new SettlementLineId(nextUuid());
    }

    @Override
    public PaymentAttemptId nextPaymentAttemptId() {
        return new PaymentAttemptId(nextUuid());
    }
}