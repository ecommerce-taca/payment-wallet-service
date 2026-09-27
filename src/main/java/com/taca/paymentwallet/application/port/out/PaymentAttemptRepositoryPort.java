package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.domain.payment.PaymentAttempt;
import com.taca.paymentwallet.domain.valueobject.PaymentId;

import java.util.List;
import java.util.Optional;

public interface PaymentAttemptRepositoryPort {

    PaymentAttempt save(PaymentAttempt attempt);

    Optional<PaymentAttempt>
    findByProviderAndProviderTransactionRef(
            String provider,
            String providerTransactionRef
    );

    List<PaymentAttempt> findByPaymentId(
            PaymentId paymentId
    );

    boolean existsPendingByPaymentId(
            PaymentId paymentId
    );
}