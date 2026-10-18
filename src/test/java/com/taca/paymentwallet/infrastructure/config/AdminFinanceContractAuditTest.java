package com.taca.paymentwallet.infrastructure.config;

import com.taca.paymentwallet.application.port.in.RunSettlementUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class AdminFinanceContractAuditTest {

    @Test
    void shouldKeepSettlementExecutionUnwiredWhileEligibilityContractIsOpen() {
        boolean runSettlementBeanExists = Arrays.stream(
                        ApplicationConfiguration.class.getDeclaredMethods()
                )
                .filter(method -> method.isAnnotationPresent(Bean.class))
                .map(Method::getReturnType)
                .anyMatch(RunSettlementUseCase.class::isAssignableFrom);

        assertThat(runSettlementBeanExists).isFalse();
    }
}