package com.taca.paymentwallet.domain.payment;

import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;

public class Payment {

    private final PaymentId id;
    private final CheckoutGroupId checkoutGroupId;
    private final Money amount;
    private PaymentStatus status;

    public Payment(
            PaymentId id,
            CheckoutGroupId checkoutGroupId,
            Money amount,
            PaymentStatus status
    ) {
        this.id = id;
        this.checkoutGroupId = checkoutGroupId;
        this.amount = amount;
        this.status = status;
    }

    public PaymentId id() {
        return id;
    }

    public CheckoutGroupId checkoutGroupId() {
        return checkoutGroupId;
    }

    public Money amount() {
        return amount;
    }

    public PaymentStatus status() {
        return status;
    }
}
