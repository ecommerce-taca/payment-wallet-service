package com.taca.paymentwallet.infrastructure.config;

import com.taca.paymentwallet.application.port.in.RunSettlementUseCase;
import com.taca.paymentwallet.presentation.rest.admin.AdminFeeController;
import com.taca.paymentwallet.presentation.rest.admin.AdminFinanceSummaryController;
import com.taca.paymentwallet.presentation.rest.admin.AdminSettlementController;
import com.taca.paymentwallet.presentation.rest.admin.AdminTaxController;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

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
    void shouldExposeAuthoritativeAdminFinanceReadEndpoints() {
        assertThat(hasGetMapping(
                AdminFeeController.class,
                "/fees"
        )).isTrue();

        assertThat(hasGetMapping(
                AdminTaxController.class,
                "/taxes"
        )).isTrue();

        assertThat(hasGetMapping(
                AdminSettlementController.class,
                "/settlements"
        )).isTrue();

        assertThat(hasGetMapping(
                AdminSettlementController.class,
                "/settlements/{batchId}"
        )).isTrue();

        assertThat(hasGetMapping(
                AdminFinanceSummaryController.class,
                "/summary"
        )).isTrue();
    }

    @Test
    void shouldExposeAuthoritativeSettlementRetryEndpoint() {
        assertThat(hasPostMapping(
                AdminSettlementController.class,
                "/settlements/{batchId}/retry"
        )).isTrue();
    }

    @Test
    void shouldExposeAuthoritativeFeeMutationEndpoint() {
        assertThat(
                hasPutMapping(
                        AdminFeeController.class,
                        "/fees"
                )
        ).isTrue();
    }

    @Test
    void shouldExposeAuthoritativeTaxMutationEndpoint() {
        assertThat(
                hasPutMapping(
                        AdminTaxController.class,
                        "/taxes"
                )
        ).isTrue();
    }

    @Test
    void shouldKeepFinanceSummaryExportUnexposedUntilStorageIsReady() {
        assertThat(hasGetMapping(
                AdminFinanceSummaryController.class,
                "/summary/export"
        )).isFalse();
    }

    private boolean hasGetMapping(
            Class<?> controller,
            String path
    ) {
        return Arrays.stream(controller.getDeclaredMethods())
                .map(method -> method.getAnnotation(GetMapping.class))
                .filter(annotation -> annotation != null)
                .flatMap(annotation -> mappings(
                        annotation.value(),
                        annotation.path()
                ))
                .anyMatch(path::equals);
    }

    private boolean hasPostMapping(
            Class<?> controller,
            String path
    ) {
        return Arrays.stream(controller.getDeclaredMethods())
                .map(method -> method.getAnnotation(PostMapping.class))
                .filter(annotation -> annotation != null)
                .flatMap(annotation -> mappings(
                        annotation.value(),
                        annotation.path()
                ))
                .anyMatch(path::equals);
    }

    private boolean hasPutMapping(
            Class<?> controller,
            String path
    ) {
        return Arrays.stream(controller.getDeclaredMethods())
                .map(method -> method.getAnnotation(PutMapping.class))
                .filter(annotation -> annotation != null)
                .flatMap(annotation -> mappings(
                        annotation.value(),
                        annotation.path()
                ))
                .anyMatch(path::equals);
    }

    private Stream<String> mappings(
            String[] value,
            String[] path
    ) {
        return Stream.concat(
                Arrays.stream(value),
                Arrays.stream(path)
        );
    }
}