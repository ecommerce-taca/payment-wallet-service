package com.taca.paymentwallet.infrastructure.config;

import com.taca.paymentwallet.application.port.out.OperationMetricsPort;
import com.taca.paymentwallet.infrastructure.health.KafkaReadinessHealthIndicator;
import com.taca.paymentwallet.infrastructure.health.OutboxBacklogHealthIndicator;
import com.taca.paymentwallet.infrastructure.health.VnpayReadinessHealthIndicator;
import com.taca.paymentwallet.infrastructure.messaging.kafka.OutboxOperationalObservation;
import com.taca.paymentwallet.infrastructure.messaging.kafka.OutboxPublisherObservation;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityContractAuditTest {

    @Test
    void shouldWireCurrentObservabilityComponents() {
        assertThat(hasInfrastructureBean(
                KafkaReadinessHealthIndicator.class
        )).isTrue();

        assertThat(hasInfrastructureBean(
                OutboxBacklogHealthIndicator.class
        )).isTrue();

        assertThat(hasInfrastructureBean(
                VnpayReadinessHealthIndicator.class
        )).isTrue();

        assertThat(hasInfrastructureBean(
                OutboxPublisherObservation.class
        )).isTrue();

        assertThat(hasInfrastructureBean(
                OutboxOperationalObservation.class
        )).isTrue();
    }

    @Test
    void shouldWireOperationMetricsFoundation() {
        assertThat(
                hasInfrastructureBean(OperationMetricsPort.class)
        ).isTrue();
    }

    private boolean hasInfrastructureBean(Class<?> type) {
        return Arrays.stream(
                        InfrastructureConfiguration.class
                                .getDeclaredMethods()
                )
                .filter(method ->
                        method.isAnnotationPresent(
                                Bean.class
                        )
                )
                .map(Method::getReturnType)
                .anyMatch(type::isAssignableFrom);
    }
}