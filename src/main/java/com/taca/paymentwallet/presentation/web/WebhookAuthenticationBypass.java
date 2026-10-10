package com.taca.paymentwallet.presentation.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public final class WebhookAuthenticationBypass {

    static final String VNPAY_WEBHOOK_PATH = "/api/v1/payments/webhook";

    public boolean shouldBypass(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && VNPAY_WEBHOOK_PATH.equals(request.getRequestURI());
    }
}