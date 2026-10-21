package com.taca.paymentwallet.infrastructure.config;

import com.taca.paymentwallet.application.port.in.RunSettlementUseCase;
import com.taca.paymentwallet.presentation.rest.admin.AdminSettlementController;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

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

    @Test
    void shouldKeepSettlementRetryEndpointUnexposedWhileRetryContractIsOpen() {
        boolean retryEndpointExists = Arrays.stream(
                        AdminSettlementController.class.getDeclaredMethods()
                )
                .map(method -> method.getAnnotation(PostMapping.class))
                .filter(annotation -> annotation != null)
                .flatMap(annotation -> Stream.concat(
                        Arrays.stream(annotation.value()),
                        Arrays.stream(annotation.path())
                ))
                .anyMatch("/settlements/{batchId}/retry"::equals);

        assertThat(retryEndpointExists).isFalse();
    }
}