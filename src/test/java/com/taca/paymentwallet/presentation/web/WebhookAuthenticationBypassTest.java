package com.taca.paymentwallet.presentation.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookAuthenticationBypassTest {

    private final WebhookAuthenticationBypass bypass = new WebhookAuthenticationBypass();

    @Test
    void shouldBypassExactVnpayWebhookPost() {
        MockHttpServletRequest request = request("POST", "/api/v1/payments/webhook");

        assertThat(bypass.shouldBypass(request)).isTrue();
    }

    @Test
    void shouldNotBypassWebhookWithWrongMethod() {
        MockHttpServletRequest request = request("GET", "/api/v1/payments/webhook");

        assertThat(bypass.shouldBypass(request)).isFalse();
    }

    @Test
    void shouldNotBypassCreatePayment() {
        MockHttpServletRequest request = request("POST", "/api/v1/payments");

        assertThat(bypass.shouldBypass(request)).isFalse();
    }

    @Test
    void shouldNotBypassNestedWebhookPath() {
        MockHttpServletRequest request = request(
                "POST",
                "/api/v1/payments/webhook/anything"
        );

        assertThat(bypass.shouldBypass(request)).isFalse();
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        return request;
    }
}