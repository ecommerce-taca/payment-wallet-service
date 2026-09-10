package com.taca.paymentwallet.application.port.out;

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
}
