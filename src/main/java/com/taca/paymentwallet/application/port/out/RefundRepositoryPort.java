package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.domain.refund.Refund;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.RefundId;

import java.util.Optional;

public interface RefundRepositoryPort {

    Optional<Refund> findById(RefundId refundId);

    Money sumPendingRefundAmount(PaymentId paymentId);

    Refund save(Refund refund);
}
