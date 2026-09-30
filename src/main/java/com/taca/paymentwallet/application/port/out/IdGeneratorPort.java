package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.domain.valueobject.*;

public interface IdGeneratorPort {

    PaymentId nextPaymentId();

    PaymentAllocationId nextPaymentAllocationId();

    WalletId nextWalletId();

    LedgerAccountId nextLedgerAccountId();

    LedgerPostingId nextLedgerPostingId();

    RefundId nextRefundId();

    PayoutId nextPayoutId();

    SettlementBatchId nextSettlementBatchId();

    SettlementBatchItemId nextSettlementBatchItemId();

    SettlementLineId nextSettlementLineId();

    PaymentAttemptId nextPaymentAttemptId();
}
