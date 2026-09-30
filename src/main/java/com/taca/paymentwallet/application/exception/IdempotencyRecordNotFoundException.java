package com.taca.paymentwallet.application.exception;

import com.taca.paymentwallet.application.idempotency.IdempotencyScope;

public class IdempotencyRecordNotFoundException extends ApplicationException {

    public IdempotencyRecordNotFoundException(
            IdempotencyScope scope,
            String idempotencyKey
    ) {
        super(
                "idempotency record not found for scope "
                        + scope.value()
                        + " and key "
                        + idempotencyKey
        );
    }
}