package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;

import java.util.Optional;

public interface PaymentRepositoryPort {

    Optional<Payment> findById(PaymentId paymentId);

    Optional<Payment> findByCheckoutGroupId(CheckoutGroupId checkoutGroupId);

    Optional<Payment> findByOrderId(OrderId orderId);

    default Optional<Payment> findByIdForUpdate(PaymentId paymentId) {
        return findById(paymentId);
    }

    default Optional<Payment> findByCheckoutGroupIdForUpdate(CheckoutGroupId checkoutGroupId) {
        return findByCheckoutGroupId(checkoutGroupId);
    }

    default Optional<Payment> findByOrderIdForUpdate(OrderId orderId) {
        return findByOrderId(orderId);
    }

    Payment save(Payment payment);
}