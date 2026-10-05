package com.taca.paymentwallet.infrastructure.health;

import com.taca.paymentwallet.infrastructure.vnpay.VnpayConfigurationValidator;
import com.taca.paymentwallet.infrastructure.vnpay.VnpayProperties;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;

import java.util.Objects;
import java.util.Set;

public class VnpayReadinessHealthIndicator
        extends AbstractHealthIndicator {

    private final VnpayProperties properties;
    private final VnpayConfigurationValidator validator;

    public VnpayReadinessHealthIndicator(
            VnpayProperties properties,
            VnpayConfigurationValidator validator
    ) {
        this.properties =
                Objects.requireNonNull(properties);

        this.validator =
                Objects.requireNonNull(validator);
    }

    @Override
    protected void doHealthCheck(
            Health.Builder builder
    ) {
        Set<String> missing =
                validator.missingRequiredProperties(
                        properties
                );

        if (!missing.isEmpty()) {
            builder.outOfService()
                    .withDetail(
                            "provider",
                            "VNPAY"
                    )
                    .withDetail(
                            "configured",
                            false
                    )
                    .withDetail(
                            "missingProperties",
                            missing
                    );

            return;
        }

        builder.up()
                .withDetail(
                        "provider",
                        "VNPAY"
                )
                .withDetail(
                        "configured",
                        true
                );
    }
}