package com.taca.paymentwallet.presentation.web;

import com.taca.paymentwallet.application.security.ActorContext;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActorHeaderParserTest {

    private final ActorHeaderParser parser = new ActorHeaderParser();

    @Test
    void shouldParseActorHeaders() {
        ActorContext actor = parser.parse(
                "10000000-0000-0000-0000-000000000001",
                "SELLER,ADMIN",
                "FINANCE_OPS,REPORT_READ",
                "20000000-0000-0000-0000-000000000001,20000000-0000-0000-0000-000000000002"
        );

        assertThat(actor).isNotNull();
        assertThat(actor.userId()).isEqualTo(UUID.fromString("10000000-0000-0000-0000-000000000001"));
        assertThat(actor.roles()).containsExactly("SELLER", "ADMIN");
        assertThat(actor.permissions()).containsExactly("FINANCE_OPS", "REPORT_READ");
        assertThat(actor.shopScope()).containsExactly(
                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                UUID.fromString("20000000-0000-0000-0000-000000000002")
        );
    }

    @Test
    void shouldTrimHeaderValues() {
        ActorContext actor = parser.parse(
                " 10000000-0000-0000-0000-000000000001 ",
                " SELLER , ADMIN ",
                " FINANCE_OPS ",
                " 20000000-0000-0000-0000-000000000001 "
        );

        assertThat(actor.roles()).containsExactly("SELLER", "ADMIN");
        assertThat(actor.permissions()).containsExactly("FINANCE_OPS");
        assertThat(actor.shopScope()).containsExactly(
                UUID.fromString("20000000-0000-0000-0000-000000000001")
        );
    }

    @Test
    void shouldAllowEmptyOptionalActorHeaders() {
        ActorContext actor = parser.parse(
                "10000000-0000-0000-0000-000000000001",
                null,
                "",
                " "
        );

        assertThat(actor).isNotNull();
        assertThat(actor.roles()).isEmpty();
        assertThat(actor.permissions()).isEmpty();
        assertThat(actor.shopScope()).isEmpty();
    }

    @Test
    void shouldReturnNullWhenUserIdIsMissing() {
        assertThat(parser.parse(null, null, null, null))
                .isNull();
    }

    @Test
    void shouldRejectInvalidUserId() {
        assertThatThrownBy(() ->
                parser.parse("invalid-user", null, null, null)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("X-User-ID must contain valid UUID values");
    }

    @Test
    void shouldRejectInvalidShopScope() {
        assertThatThrownBy(() ->
                parser.parse(
                        "10000000-0000-0000-0000-000000000001",
                        null,
                        null,
                        "invalid-shop"
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("X-User-Shop-Scope must contain valid UUID values");
    }
}