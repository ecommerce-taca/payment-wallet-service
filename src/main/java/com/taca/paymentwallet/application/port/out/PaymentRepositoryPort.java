package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;

import java.util.Optional;

public interface PaymentRepositoryPort {

    Optional<Payment> findById(PaymentId paymentId);

    Optional<Payment> findByCheckoutGroupId(CheckoutGroupId checkoutGroupId);

    default Optional<Payment> findByIdForUpdate(PaymentId paymentId) {
        return findById(paymentId);
    }

    Payment save(Payment payment);
}
