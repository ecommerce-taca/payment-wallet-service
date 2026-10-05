package com.taca.paymentwallet.application.security;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActorContextTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    private static final UUID SHOP_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Test
    void shouldCreateActorContext() {
        ActorContext actor =
                new ActorContext(
                        USER_ID,
                        Set.of("SELLER"),
                        Set.of("FINANCE_OPS"),
                        Set.of(SHOP_ID)
                );

        assertThat(actor.userId()).isEqualTo(USER_ID);

        assertThat(actor.hasRole("SELLER")).isTrue();

        assertThat(
                actor.hasPermission("FINANCE_OPS")
        ).isTrue();

        assertThat(
                actor.hasShopAccess(SHOP_ID)
        ).isTrue();
    }

    @Test
    void shouldNormalizeRoleAndPermissionValues() {
        ActorContext actor =
                new ActorContext(
                        USER_ID,
                        Set.of(" SELLER "),
                        Set.of(" FINANCE_OPS "),
                        Set.of()
                );

        assertThat(actor.roles()).containsExactly("SELLER");

        assertThat(actor.permissions()).containsExactly("FINANCE_OPS");
    }

    @Test
    void shouldDefensivelyCopyCollections() {
        Set<String> roles = new LinkedHashSet<>();

        roles.add("SELLER");

        Set<String> permissions = new LinkedHashSet<>();

        permissions.add("FINANCE_OPS");

        Set<UUID> shopScope = new LinkedHashSet<>();

        shopScope.add(SHOP_ID);

        ActorContext actor =
                new ActorContext(
                        USER_ID,
                        roles,
                        permissions,
                        shopScope
                );

        roles.add("ADMIN");

        permissions.clear();

        shopScope.clear();

        assertThat(actor.roles()).containsExactly("SELLER");

        assertThat(actor.permissions()).containsExactly("FINANCE_OPS");

        assertThat(actor.shopScope())
                .containsExactly(
                        SHOP_ID
                );
    }

    @Test
    void shouldRejectNullUserId() {
        assertThatThrownBy(
                () -> new ActorContext(
                        null,
                        Set.of(),
                        Set.of(),
                        Set.of()
                )
        )
                .isInstanceOf(
                        NullPointerException.class
                )
                .hasMessage(
                        "userId must not be null"
                );
    }

    @Test
    void shouldRejectBlankRole() {
        assertThatThrownBy(
                () -> new ActorContext(
                        USER_ID,
                        Set.of(" "),
                        Set.of(),
                        Set.of()
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "roles must not contain blank values"
                );
    }

    @Test
    void shouldRejectBlankPermission() {
        assertThatThrownBy(
                () -> new ActorContext(
                        USER_ID,
                        Set.of(),
                        Set.of(" "),
                        Set.of()
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "permissions must not contain blank values"
                );
    }

    @Test
    void shouldAllowEmptyRolesPermissionsAndShopScope() {
        ActorContext actor =
                new ActorContext(
                        USER_ID,
                        Set.of(),
                        Set.of(),
                        Set.of()
                );

        assertThat(actor.roles()).isEmpty();

        assertThat(actor.permissions()).isEmpty();

        assertThat(actor.shopScope()).isEmpty();
    }

    @Test
    void shouldReturnFalseForUnknownAuthorizationValues() {
        ActorContext actor =
                new ActorContext(
                        USER_ID,
                        Set.of("BUYER"),
                        Set.of(),
                        Set.of()
                );

        assertThat(
                actor.hasRole("SELLER")
        ).isFalse();

        assertThat(
                actor.hasPermission("FINANCE_OPS")
        ).isFalse();

        assertThat(
                actor.hasShopAccess(SHOP_ID)
        ).isFalse();
    }
}