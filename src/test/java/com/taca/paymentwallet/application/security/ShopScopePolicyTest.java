package com.taca.paymentwallet.application.security;

import com.taca.paymentwallet.application.exception.ForbiddenException;
import com.taca.paymentwallet.application.exception.UnauthenticatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShopScopePolicyTest {

    private static final UUID USER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");

    private static final UUID SHOP_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000001");

    private static final UUID OTHER_SHOP_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000002");

    private final AuthenticationGuard authenticationGuard = new AuthenticationGuard();
    private final ShopScopePolicy policy = new ShopScopePolicy(authenticationGuard);

    @AfterEach
    void tearDown() {
        ActorContextHolder.clear();
    }

    @Test
    void shouldAllowShopInsideActorScope() {
        ActorContext actor = actor(Set.of(SHOP_ID, OTHER_SHOP_ID));
        ActorContextHolder.set(actor);

        assertThat(policy.requireShopAccess(SHOP_ID)).isSameAs(actor);
    }

    @Test
    void shouldRejectShopOutsideActorScope() {
        ActorContextHolder.set(actor(Set.of(SHOP_ID)));

        assertThatThrownBy(() -> policy.requireShopAccess(OTHER_SHOP_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access is forbidden");
    }

    @Test
    void shouldRejectEmptyShopScope() {
        ActorContextHolder.set(actor(Set.of()));

        assertThatThrownBy(() -> policy.requireShopAccess(SHOP_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldRejectAnonymousActor() {
        assertThatThrownBy(() -> policy.requireShopAccess(SHOP_ID))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void shouldRejectNullShopId() {
        ActorContextHolder.set(actor(Set.of(SHOP_ID)));

        assertThatThrownBy(() -> policy.requireShopAccess(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("shopId must not be null");
    }

    private ActorContext actor(Set<UUID> shopScope) {
        return new ActorContext(
                USER_ID,
                Set.of("SELLER"),
                Set.of(),
                shopScope
        );
    }
}