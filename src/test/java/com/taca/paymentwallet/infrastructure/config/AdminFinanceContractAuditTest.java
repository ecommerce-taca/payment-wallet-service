package com.taca.paymentwallet.infrastructure.config;

import com.taca.paymentwallet.application.port.in.DownloadAdminFinanceSummaryExportUseCase;
import com.taca.paymentwallet.application.port.in.ExportAdminFinanceSummaryUseCase;
import com.taca.paymentwallet.application.port.in.RetryAdminSettlementUseCase;
import com.taca.paymentwallet.application.port.in.RunSettlementUseCase;
import com.taca.paymentwallet.infrastructure.export.TemporaryAdminFinanceSummaryExportStorageAdapter;
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
    void shouldExposeFinanceSummaryExportEndpoint() {
        assertThat(hasGetMapping(
                AdminFinanceSummaryController.class,
                "/summary/export"
        )).isTrue();
    }

    @Test
    void shouldWireSettlementRetryApplication() {
        assertThat(
                hasBeanAssignableTo(RetryAdminSettlementUseCase.class)
        ).isTrue();
    }

    @Test
    void shouldWireFinanceSummaryExportApplication() {
        assertThat(
                hasBeanAssignableTo(ExportAdminFinanceSummaryUseCase.class)
        ).isTrue();

        assertThat(
                hasBeanAssignableTo(DownloadAdminFinanceSummaryExportUseCase.class)
        ).isTrue();
    }

    @Test
    void shouldWireFinanceSummaryExportStorage() {
        boolean beanExists = Arrays.stream(
                        InfrastructureConfiguration.class.getDeclaredMethods()
                )
                .filter(method -> method.isAnnotationPresent(Bean.class))
                .map(Method::getReturnType)
                .anyMatch(
                        TemporaryAdminFinanceSummaryExportStorageAdapter.class
                                ::isAssignableFrom
                );

        assertThat(beanExists).isTrue();
    }

    @Test
    void shouldExposeFinanceSummaryExportDownloadEndpoint() {
        assertThat(hasGetMapping(
                AdminFinanceSummaryController.class,
                "/summary/exports/{token}"
        )).isTrue();
    }

    private boolean hasBeanAssignableTo(Class<?> type) {
        return Arrays.stream(ApplicationConfiguration.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Bean.class))
                .map(Method::getReturnType)
                .anyMatch(type::isAssignableFrom);
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