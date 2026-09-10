package com.taca.paymentwallet.domain.refund;

import com.taca.paymentwallet.domain.exception.DomainException;
import com.taca.paymentwallet.domain.valueobject.Money;

public class RefundLimitExceededException extends DomainException {

    public RefundLimitExceededException(Money capturedAmount, Money totalRefundAmount) {
        super("Refund amount exceeded captured amount: captured="
                + capturedAmount.amount()
                + ", totalRefund="
                + totalRefundAmount.amount());
    }
}
