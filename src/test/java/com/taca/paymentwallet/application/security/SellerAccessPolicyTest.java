package com.taca.paymentwallet.application.security;

import com.taca.paymentwallet.application.exception.ForbiddenException;
import com.taca.paymentwallet.application.exception.UnauthenticatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SellerAccessPolicyTest {

    private static final UUID USER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");

    private static final UUID SHOP_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000001");

    private static final UUID OTHER_SHOP_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000002");

    private final SellerAccessPolicy policy =
            new SellerAccessPolicy(new AuthenticationGuard());

    @AfterEach
    void tearDown() {
        ActorContextHolder.clear();
    }

    @Test
    void shouldReturnShopIdForSellerWithSingleShop() {
        ActorContextHolder.set(actor(
                Set.of(SecurityRoles.SELLER),
                Set.of(SHOP_ID)
        ));

        assertThat(policy.requireSellerShopId()).isEqualTo(SHOP_ID);
    }

    @Test
    void shouldRejectAnonymousActor() {
        assertThatThrownBy(policy::requireSellerShopId)
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void shouldRejectActorWithoutSellerRole() {
        ActorContextHolder.set(actor(
                Set.of(SecurityRoles.FINANCE_OPS),
                Set.of(SHOP_ID)
        ));

        assertThatThrownBy(policy::requireSellerShopId)
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldRejectSellerWithoutShop() {
        ActorContextHolder.set(actor(
                Set.of(SecurityRoles.SELLER),
                Set.of()
        ));

        assertThatThrownBy(policy::requireSellerShopId)
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldRejectSellerWithMultipleShops() {
        ActorContextHolder.set(actor(
                Set.of(SecurityRoles.SELLER),
                Set.of(SHOP_ID, OTHER_SHOP_ID)
        ));

        assertThatThrownBy(policy::requireSellerShopId)
                .isInstanceOf(ForbiddenException.class);
    }

    private ActorContext actor(Set<String> roles, Set<UUID> shopScope) {
        return new ActorContext(USER_ID, roles, Set.of(), shopScope);
    }
}