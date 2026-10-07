package com.taca.paymentwallet.application.security;

import com.taca.paymentwallet.application.exception.ForbiddenException;
import com.taca.paymentwallet.application.exception.UnauthenticatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalCallerPolicyTest {

    private static final UUID USER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");

    private final AuthenticationGuard authenticationGuard = new AuthenticationGuard();
    private final InternalCallerPolicy policy = new InternalCallerPolicy(authenticationGuard);

    @AfterEach
    void tearDown() {
        InternalCallerContextHolder.clear();
        ActorContextHolder.clear();
    }

    @Test
    void shouldAllowOrderCommerceToCreatePayment() {
        setOrderCommerceCaller();

        assertThatCode(policy::requireOrderCommerce)
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectAnonymousCreatePaymentCaller() {
        assertThatThrownBy(policy::requireOrderCommerce)
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void shouldRejectAuthenticatedUserFromCreatingPayment() {
        ActorContextHolder.set(actor(Set.of("SELLER")));

        assertThatThrownBy(policy::requireOrderCommerce)
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldAllowOrderCommerceToRequestRefund() {
        setOrderCommerceCaller();

        assertThatCode(policy::requireOrderCommerceOrFinanceOps)
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAllowFinanceOpsToRequestRefund() {
        ActorContextHolder.set(actor(Set.of(SecurityRoles.FINANCE_OPS)));

        assertThatCode(policy::requireOrderCommerceOrFinanceOps)
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectNonFinanceActorFromRequestingRefund() {
        ActorContextHolder.set(actor(Set.of("SELLER")));

        assertThatThrownBy(policy::requireOrderCommerceOrFinanceOps)
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldRejectAnonymousRefundCaller() {
        assertThatThrownBy(policy::requireOrderCommerceOrFinanceOps)
                .isInstanceOf(UnauthenticatedException.class);
    }

    private void setOrderCommerceCaller() {
        InternalCallerContextHolder.set(
                new InternalCallerContext(InternalService.ORDER_COMMERCE)
        );
    }

    private ActorContext actor(Set<String> roles) {
        return new ActorContext(USER_ID, roles, Set.of(), Set.of());
    }
}