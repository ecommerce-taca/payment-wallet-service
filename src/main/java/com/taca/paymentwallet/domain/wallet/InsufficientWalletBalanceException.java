package com.taca.paymentwallet.domain.wallet;

import com.taca.paymentwallet.domain.exception.DomainException;
import com.taca.paymentwallet.domain.valueobject.Money;

public class InsufficientWalletBalanceException extends DomainException {

    public InsufficientWalletBalanceException(Money currentBalance, Money requestedAmount) {
        super("Insufficient wallet balance: current="
                + currentBalance.amount()
                + ", requested="
                + requestedAmount.amount());
    }
}
