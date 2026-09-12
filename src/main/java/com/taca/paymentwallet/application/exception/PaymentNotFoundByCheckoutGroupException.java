package com.taca.paymentwallet.application.exception;

import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;

public class PaymentNotFoundByCheckoutGroupException extends ApplicationException {

    public PaymentNotFoundByCheckoutGroupException(CheckoutGroupId checkoutGroupId) {
        super("Payment not found for checkout group: " + checkoutGroupId.value());
    }
}
