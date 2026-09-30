package com.taca.paymentwallet.infrastructure.vnpay;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vnpay")
public record VnpayProperties(
        String tmnCode,
        String hashSecret,
        String paymentUrl,
        String returnUrl
) {
}