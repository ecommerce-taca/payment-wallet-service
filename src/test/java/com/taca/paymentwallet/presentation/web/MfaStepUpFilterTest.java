package com.taca.paymentwallet.presentation.web;

import com.taca.paymentwallet.application.security.MfaStepUpContext;
import com.taca.paymentwallet.application.security.MfaStepUpContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MfaStepUpFilterTest {

    private final MfaStepUpFilter filter = new MfaStepUpFilter();

    @AfterEach
    void tearDown() {
        MfaStepUpContextHolder.clear();
    }

    @Test
    void shouldExposeVerifiedStepUpDuringFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-MFA-Step-Up", "verified-step-up-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<MfaStepUpContext> captured = new AtomicReference<>();

        FilterChain chain = (req, res) ->
                captured.set(MfaStepUpContextHolder.current().orElseThrow());

        filter.doFilter(request, response, chain);

        assertThat(captured.get().verified()).isTrue();
        assertThat(MfaStepUpContextHolder.current()).isEmpty();
    }

    @Test
    void shouldLeaveContextEmptyWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Boolean> present = new AtomicReference<>();

        FilterChain chain = (req, res) ->
                present.set(MfaStepUpContextHolder.current().isPresent());

        filter.doFilter(request, response, chain);

        assertThat(present.get()).isFalse();
        assertThat(MfaStepUpContextHolder.current()).isEmpty();
    }

    @Test
    void shouldClearContextWhenFilterChainFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-MFA-Step-Up", "verified-step-up-token");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            throw new ServletException("boom");
        };

        try {
            filter.doFilter(request, response, chain);
        } catch (Exception ignored) {
        }

        assertThat(MfaStepUpContextHolder.current()).isEmpty();
    }

    @Test
    void shouldAllowWebhookWithoutMfaHeader() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/payments/webhook");

        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Boolean> invoked = new AtomicReference<>(false);

        FilterChain chain = (req, res) -> {
            invoked.set(true);
            assertThat(MfaStepUpContextHolder.current()).isEmpty();
        };

        filter.doFilter(request, response, chain);

        assertThat(invoked.get()).isTrue();
        assertThat(MfaStepUpContextHolder.current()).isEmpty();
    }
}