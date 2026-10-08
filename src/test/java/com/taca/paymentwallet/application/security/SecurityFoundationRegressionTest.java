package com.taca.paymentwallet.application.security;

import com.taca.paymentwallet.application.exception.ForbiddenException;
import com.taca.paymentwallet.application.exception.MfaRequiredException;
import com.taca.paymentwallet.application.exception.UnauthenticatedException;
import com.taca.paymentwallet.presentation.web.WebhookAuthenticationBypass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityFoundationRegressionTest {

    private static final UUID USER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");

    private static final UUID SHOP_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000001");

    private static final UUID OTHER_SHOP_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000002");

    private final AuthenticationGuard authenticationGuard = new AuthenticationGuard();
    private final AuthorizationPolicy authorizationPolicy =
            new AuthorizationPolicy(authenticationGuard);
    private final ShopScopePolicy shopScopePolicy =
            new ShopScopePolicy(authenticationGuard);
    private final MfaStepUpPolicy mfaStepUpPolicy = new MfaStepUpPolicy();
    private final InternalCallerPolicy internalCallerPolicy =
            new InternalCallerPolicy(authenticationGuard);
    private final WebhookAuthenticationBypass webhookBypass =
            new WebhookAuthenticationBypass();

    @AfterEach
    void tearDown() {
        ActorContextHolder.clear();
        MfaStepUpContextHolder.clear();
        InternalCallerContextHolder.clear();
    }

    @Test
    void shouldRejectAnonymousProtectedAccess() {
        assertThatThrownBy(authenticationGuard::requireActor)
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void shouldRejectActorWithoutRequiredRole() {
        ActorContextHolder.set(actor(Set.of("SELLER"), Set.of(SHOP_ID)));

        assertThatThrownBy(() ->
                authorizationPolicy.requireRole(SecurityRoles.FINANCE_OPS)
        ).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldRejectCrossShopAccess() {
        ActorContextHolder.set(actor(Set.of("SELLER"), Set.of(SHOP_ID)));

        assertThatThrownBy(() ->
                shopScopePolicy.requireShopAccess(OTHER_SHOP_ID)
        ).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldRequireMfaForSensitiveOperation() {
        assertThatThrownBy(mfaStepUpPolicy::requireVerifiedStepUp)
                .isInstanceOf(MfaRequiredException.class);

        MfaStepUpContextHolder.set(new MfaStepUpContext(true));

        assertThatCode(mfaStepUpPolicy::requireVerifiedStepUp)
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAllowOnlyOrderCommerceForCreatePaymentPolicy() {
        ActorContextHolder.set(actor(Set.of(SecurityRoles.FINANCE_OPS), Set.of()));

        assertThatThrownBy(internalCallerPolicy::requireOrderCommerce)
                .isInstanceOf(ForbiddenException.class);

        ActorContextHolder.clear();
        InternalCallerContextHolder.set(
                new InternalCallerContext(InternalService.ORDER_COMMERCE)
        );

        assertThatCode(internalCallerPolicy::requireOrderCommerce)
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAllowOrderCommerceOrFinanceOpsForRefundPolicy() {
        InternalCallerContextHolder.set(
                new InternalCallerContext(InternalService.ORDER_COMMERCE)
        );

        assertThatCode(internalCallerPolicy::requireOrderCommerceOrFinanceOps)
                .doesNotThrowAnyException();

        InternalCallerContextHolder.clear();
        ActorContextHolder.set(
                actor(Set.of(SecurityRoles.FINANCE_OPS), Set.of())
        );

        assertThatCode(internalCallerPolicy::requireOrderCommerceOrFinanceOps)
                .doesNotThrowAnyException();

        ActorContextHolder.set(actor(Set.of("SELLER"), Set.of(SHOP_ID)));

        assertThatThrownBy(
                internalCallerPolicy::requireOrderCommerceOrFinanceOps
        ).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldBypassAuthenticationOnlyForExactVnpayWebhook() {
        assertThat(webhookBypass.shouldBypass(
                request("POST", "/api/v1/payments/webhook")
        )).isTrue();

        assertThat(webhookBypass.shouldBypass(
                request("GET", "/api/v1/payments/webhook")
        )).isFalse();

        assertThat(webhookBypass.shouldBypass(
                request("POST", "/api/v1/payments")
        )).isFalse();

        assertThat(webhookBypass.shouldBypass(
                request("POST", "/api/v1/payments/webhook/test")
        )).isFalse();
    }

    @Test
    void shouldKeepSecurityContextsIndependent() {
        ActorContext actor = actor(Set.of("SELLER"), Set.of(SHOP_ID));

        ActorContextHolder.set(actor);
        MfaStepUpContextHolder.set(new MfaStepUpContext(true));
        InternalCallerContextHolder.set(
                new InternalCallerContext(InternalService.ORDER_COMMERCE)
        );

        ActorContextHolder.clear();

        assertThat(ActorContextHolder.current()).isEmpty();
        assertThat(MfaStepUpContextHolder.current()).isPresent();
        assertThat(InternalCallerContextHolder.current()).isPresent();

        MfaStepUpContextHolder.clear();

        assertThat(MfaStepUpContextHolder.current()).isEmpty();
        assertThat(InternalCallerContextHolder.current()).isPresent();

        InternalCallerContextHolder.clear();

        assertThat(InternalCallerContextHolder.current()).isEmpty();
    }

    private ActorContext actor(Set<String> roles, Set<UUID> shopScope) {
        return new ActorContext(USER_ID, roles, Set.of(), shopScope);
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        return request;
    }
}