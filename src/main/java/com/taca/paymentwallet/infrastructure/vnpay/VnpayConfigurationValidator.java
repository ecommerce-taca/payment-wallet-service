package com.taca.paymentwallet.infrastructure.vnpay;

import java.util.LinkedHashSet;
import java.util.Set;

public class VnpayConfigurationValidator {

    public Set<String> missingRequiredProperties(
            VnpayProperties properties
    ) {
        Set<String> missing =
                new LinkedHashSet<>();

        if (isBlank(properties.tmnCode())) {
            missing.add("tmnCode");
        }

        if (isBlank(properties.hashSecret())) {
            missing.add("hashSecret");
        }

        if (isBlank(properties.paymentUrl())) {
            missing.add("paymentUrl");
        }

        if (isBlank(properties.returnUrl())) {
            missing.add("returnUrl");
        }

        return Set.copyOf(missing);
    }

    public void validateForPaymentUrl(
            VnpayProperties properties
    ) {
        Set<String> missing =
                missingRequiredProperties(properties);

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Missing required VNPAY configuration: "
                            + String.join(", ", missing)
            );
        }
    }

    public void validateForWebhook(
            VnpayProperties properties
    ) {
        if (isBlank(properties.hashSecret())) {
            throw new IllegalStateException(
                    "VNPAY hashSecret must be configured"
            );
        }
    }

    private boolean isBlank(
            String value
    ) {
        return value == null
                || value.isBlank();
    }
}