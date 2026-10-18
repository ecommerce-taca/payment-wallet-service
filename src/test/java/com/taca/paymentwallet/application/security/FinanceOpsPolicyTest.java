package com.taca.paymentwallet.application.security;

import com.taca.paymentwallet.application.exception.ForbiddenException;
import com.taca.paymentwallet.application.exception.MfaRequiredException;
import com.taca.paymentwallet.application.exception.UnauthenticatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinanceOpsPolicyTest {

    private final AuthenticationGuard authenticationGuard =
            new AuthenticationGuard();

    private final AuthorizationPolicy authorizationPolicy =
            new AuthorizationPolicy(authenticationGuard);

    private final MfaStepUpPolicy mfaStepUpPolicy =
            new MfaStepUpPolicy();

    private final FinanceOpsPolicy policy =
            new FinanceOpsPolicy(
                    authorizationPolicy,
                    mfaStepUpPolicy
            );

    @AfterEach
    void tearDown() {
        ActorContextHolder.clear();
        MfaStepUpContextHolder.clear();
    }

    @Test
    void shouldAllowFinanceOpsActor() {
        ActorContext actor = actor(
                Set.of(SecurityRoles.FINANCE_OPS)
        );

        ActorContextHolder.set(actor);

        assertThat(policy.requireFinanceOps())
                .isSameAs(actor);
    }

    @Test
    void shouldRejectAnonymousActor() {
        assertThatThrownBy(policy::requireFinanceOps)
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void shouldRejectActorWithoutFinanceOpsRole() {
        ActorContextHolder.set(
                actor(Set.of(SecurityRoles.SELLER))
        );

        assertThatThrownBy(policy::requireFinanceOps)
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access is forbidden");
    }

    @Test
    void shouldAllowFinanceOpsActorWithVerifiedStepUp() {
        ActorContext actor = actor(
                Set.of(SecurityRoles.FINANCE_OPS)
        );

        ActorContextHolder.set(actor);
        MfaStepUpContextHolder.set(
                new MfaStepUpContext(true)
        );

        assertThat(policy.requireFinanceOpsWithStepUp())
                .isSameAs(actor);
    }

    @Test
    void shouldRejectFinanceOpsActorWithoutStepUp() {
        ActorContextHolder.set(
                actor(Set.of(SecurityRoles.FINANCE_OPS))
        );

        assertThatThrownBy(
                policy::requireFinanceOpsWithStepUp
        )
                .isInstanceOf(MfaRequiredException.class)
                .hasMessage("MFA step-up is required");
    }

    @Test
    void shouldRejectNonFinanceOpsBeforeStepUpCheck() {
        ActorContextHolder.set(
                actor(Set.of(SecurityRoles.SELLER))
        );

        MfaStepUpContextHolder.set(
                new MfaStepUpContext(true)
        );

        assertThatThrownBy(
                policy::requireFinanceOpsWithStepUp
        )
                .isInstanceOf(ForbiddenException.class);
    }

    private ActorContext actor(Set<String> roles) {
        return new ActorContext(
                UUID.fromString(
                        "20000000-0000-0000-0000-000000000001"
                ),
                roles,
                Set.of(),
                Set.of()
        );
    }
}