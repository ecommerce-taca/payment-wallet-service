package com.taca.paymentwallet.presentation.web;

import com.taca.paymentwallet.application.security.ActorContext;
import com.taca.paymentwallet.application.security.ActorContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ActorContextFilterTest {

    private final ActorContextFilter filter = new ActorContextFilter();

    @AfterEach
    void tearDown() {
        ActorContextHolder.clear();
    }

    @Test
    void shouldExposeActorContextDuringFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-ID", "10000000-0000-0000-0000-000000000001");
        request.addHeader("X-User-Roles", "SELLER");
        request.addHeader("X-User-Permissions", "FINANCE_OPS");
        request.addHeader("X-User-Shop-Scope", "20000000-0000-0000-0000-000000000001");

        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<ActorContext> captured = new AtomicReference<>();

        FilterChain chain = (req, res) ->
                captured.set(ActorContextHolder.current().orElseThrow());

        filter.doFilter(request, response, chain);

        assertThat(captured.get().hasRole("SELLER")).isTrue();
        assertThat(captured.get().hasPermission("FINANCE_OPS")).isTrue();
        assertThat(captured.get().shopScope()).hasSize(1);
        assertThat(ActorContextHolder.current()).isEmpty();
    }

    @Test
    void shouldLeaveContextEmptyForAnonymousRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Boolean> contextPresent = new AtomicReference<>();

        FilterChain chain = (req, res) ->
                contextPresent.set(ActorContextHolder.current().isPresent());

        filter.doFilter(request, response, chain);

        assertThat(contextPresent.get()).isFalse();
        assertThat(ActorContextHolder.current()).isEmpty();
    }

    @Test
    void shouldClearContextWhenFilterChainFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-ID", "10000000-0000-0000-0000-000000000001");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            throw new ServletException("boom");
        };

        try {
            filter.doFilter(request, response, chain);
        } catch (Exception ignored) {
        }

        assertThat(ActorContextHolder.current()).isEmpty();
    }

    @Test
    void shouldAllowWebhookWithoutActorHeaders() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/payments/webhook");

        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Boolean> invoked = new AtomicReference<>(false);

        FilterChain chain = (req, res) -> {
            invoked.set(true);
            assertThat(ActorContextHolder.current()).isEmpty();
        };

        filter.doFilter(request, response, chain);

        assertThat(invoked.get()).isTrue();
        assertThat(ActorContextHolder.current()).isEmpty();
    }
}