package com.taca.paymentwallet.application.security;

import com.taca.paymentwallet.application.exception.ForbiddenException;
import com.taca.paymentwallet.application.result.GetPaymentResult;

import java.util.Objects;

public class PaymentVisibilityPolicy {

    private final AuthenticationGuard authenticationGuard;

    public PaymentVisibilityPolicy(
            AuthenticationGuard authenticationGuard
    ) {
        this.authenticationGuard =
                Objects.requireNonNull(authenticationGuard);
    }

    public void requireCanView(
            GetPaymentResult payment
    ) {
        Objects.requireNonNull(
                payment,
                "payment must not be null"
        );

        if (isOrderCommerceInternal()) {
            return;
        }

        ActorContext actor = authenticationGuard.requireActor();

        if (actor.hasRole(SecurityRoles.FINANCE_OPS)) {
            return;
        }

        if (actor.userId()
                .equals(payment.buyerUserId())) {
            return;
        }

        throw new ForbiddenException();
    }

    private boolean isOrderCommerceInternal() {
        return InternalCallerContextHolder.current()
                .map(context ->
                        context.service()
                                == InternalService.ORDER_COMMERCE
                )
                .orElse(false);
    }
}