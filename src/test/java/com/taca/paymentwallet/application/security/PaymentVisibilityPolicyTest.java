package com.taca.paymentwallet.application.security;

import com.taca.paymentwallet.application.exception.ForbiddenException;
import com.taca.paymentwallet.application.exception.UnauthenticatedException;
import com.taca.paymentwallet.application.result.GetPaymentOrderResult;
import com.taca.paymentwallet.application.result.GetPaymentResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentVisibilityPolicyTest {

    @AfterEach
    void clearContexts() {
        ActorContextHolder.clear();
        InternalCallerContextHolder.clear();
    }

    @Test
    void shouldAllowOrderCommerceInternalCaller() {
        InternalCallerContextHolder.set(
                new InternalCallerContext(
                        InternalService.ORDER_COMMERCE
                )
        );

        PaymentVisibilityPolicy policy =
                newPolicy();

        assertDoesNotThrow(
                () -> policy.requireCanView(
                        payment(UUID.randomUUID())
                )
        );
    }

    @Test
    void shouldAllowBuyerToViewOwnPayment() {
        UUID buyerUserId =
                UUID.randomUUID();

        ActorContextHolder.set(
                actor(
                        buyerUserId,
                        Set.of("BUYER")
                )
        );

        PaymentVisibilityPolicy policy =
                newPolicy();

        assertDoesNotThrow(
                () -> policy.requireCanView(
                        payment(buyerUserId)
                )
        );
    }

    @Test
    void shouldRejectBuyerViewingAnotherUsersPayment() {
        ActorContextHolder.set(
                actor(
                        UUID.randomUUID(),
                        Set.of("BUYER")
                )
        );

        PaymentVisibilityPolicy policy =
                newPolicy();

        assertThrows(
                ForbiddenException.class,
                () -> policy.requireCanView(
                        payment(UUID.randomUUID())
                )
        );
    }

    @Test
    void shouldAllowFinanceOps() {
        ActorContextHolder.set(
                actor(
                        UUID.randomUUID(),
                        Set.of(
                                SecurityRoles.FINANCE_OPS
                        )
                )
        );

        PaymentVisibilityPolicy policy =
                newPolicy();

        assertDoesNotThrow(
                () -> policy.requireCanView(
                        payment(UUID.randomUUID())
                )
        );
    }

    @Test
    void shouldRejectSellerOnGenericPaymentDetail() {
        ActorContextHolder.set(
                actor(
                        UUID.randomUUID(),
                        Set.of("SELLER")
                )
        );

        PaymentVisibilityPolicy policy =
                newPolicy();

        assertThrows(
                ForbiddenException.class,
                () -> policy.requireCanView(
                        payment(UUID.randomUUID())
                )
        );
    }

    @Test
    void shouldRejectSellerStaffOnGenericPaymentDetail() {
        ActorContextHolder.set(
                actor(
                        UUID.randomUUID(),
                        Set.of("SELLER_STAFF")
                )
        );

        PaymentVisibilityPolicy policy =
                newPolicy();

        assertThrows(
                ForbiddenException.class,
                () -> policy.requireCanView(
                        payment(UUID.randomUUID())
                )
        );
    }

    @Test
    void shouldRejectAnonymousCaller() {
        PaymentVisibilityPolicy policy =
                newPolicy();

        assertThrows(
                UnauthenticatedException.class,
                () -> policy.requireCanView(
                        payment(UUID.randomUUID())
                )
        );
    }

    private PaymentVisibilityPolicy newPolicy() {
        return new PaymentVisibilityPolicy(
                new AuthenticationGuard()
        );
    }

    private ActorContext actor(
            UUID userId,
            Set<String> roles
    ) {
        return new ActorContext(
                userId,
                roles,
                Set.of(),
                Set.of()
        );
    }

    private GetPaymentResult payment(
            UUID buyerUserId
    ) {
        return new GetPaymentResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                buyerUserId,
                "PENDING_COD",
                "COD",
                100_000,
                "VND",
                0,
                0,
                null,
                null,
                List.of(
                        new GetPaymentOrderResult(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                90_000,
                                10_000,
                                100_000
                        )
                )
        );
    }
}