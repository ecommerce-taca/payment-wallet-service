package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.idempotency.IdempotencyRecord;
import com.taca.paymentwallet.application.idempotency.IdempotencyScope;

import java.util.Optional;

public interface IdempotencyPort {

    Optional<IdempotencyRecord> find(
            IdempotencyScope scope,
            String idempotencyKey
    );

    IdempotencyRecord reserve(
            IdempotencyScope scope,
            String idempotencyKey,
            String requestHash
    );

    void markSucceeded(
            IdempotencyScope scope,
            String idempotencyKey,
            String responsePayload
    );

    void markFailed(
            IdempotencyScope scope,
            String idempotencyKey,
            String failureCode
    );
}
