package com.taca.paymentwallet.application.security;

import com.taca.paymentwallet.application.exception.ForbiddenException;
import com.taca.paymentwallet.application.exception.UnauthenticatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationPolicyTest {

    private final AuthenticationGuard authenticationGuard = new AuthenticationGuard();
    private final AuthorizationPolicy policy = new AuthorizationPolicy(authenticationGuard);

    @AfterEach
    void tearDown() {
        ActorContextHolder.clear();
    }

    @Test
    void shouldAllowRequiredRole() {
        ActorContext actor = actor(
                Set.of(SecurityRoles.FINANCE_OPS),
                Set.of()
        );

        ActorContextHolder.set(actor);

        assertThat(policy.requireRole(SecurityRoles.FINANCE_OPS))
                .isSameAs(actor);
    }

    @Test
    void shouldRejectMissingRole() {
        ActorContextHolder.set(actor(Set.of("SELLER"), Set.of()));

        assertThatThrownBy(() -> policy.requireRole(SecurityRoles.FINANCE_OPS))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access is forbidden");
    }

    @Test
    void shouldAllowRequiredPermission() {
        ActorContext actor = actor(
                Set.of("ADMIN"),
                Set.of("REPORT_READ")
        );

        ActorContextHolder.set(actor);

        assertThat(policy.requirePermission("REPORT_READ"))
                .isSameAs(actor);
    }

    @Test
    void shouldRejectMissingPermission() {
        ActorContextHolder.set(actor(Set.of("ADMIN"), Set.of()));

        assertThatThrownBy(() -> policy.requirePermission("REPORT_READ"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldRejectAnonymousActorBeforeAuthorization() {
        assertThatThrownBy(() -> policy.requireRole(SecurityRoles.FINANCE_OPS))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void shouldRejectBlankAuthorityConfiguration() {
        ActorContextHolder.set(actor(Set.of("SELLER"), Set.of()));

        assertThatThrownBy(() -> policy.requireRole(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("role must not be blank");

        assertThatThrownBy(() -> policy.requirePermission(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("permission must not be blank");
    }

    private ActorContext actor(Set<String> roles, Set<String> permissions) {
        return new ActorContext(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                roles,
                permissions,
                Set.of()
        );
    }
}