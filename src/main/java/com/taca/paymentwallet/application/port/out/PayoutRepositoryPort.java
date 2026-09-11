package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.domain.payout.Payout;
import com.taca.paymentwallet.domain.valueobject.PayoutId;

import java.util.Optional;

public interface PayoutRepositoryPort {

    Optional<Payout> findById(PayoutId payoutId);

    default Optional<Payout> findByIdForUpdate(PayoutId payoutId) {
        return findById(payoutId);
    }

    Payout save(Payout payout);
}