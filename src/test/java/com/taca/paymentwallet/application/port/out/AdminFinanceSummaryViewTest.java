package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.infrastructure.config.InfrastructureConfiguration;
import com.taca.paymentwallet.infrastructure.export.TemporaryAdminFinanceSummaryExportStorageAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class AdminFinanceSummaryViewTest {

    @Test
    void shouldAcceptValidSummary() {
        AdminFinanceSummaryView view =
                new AdminFinanceSummaryView(
                        1_000_000L,
                        100_000L,
                        900_000L,
                        90_000L,
                        45_000L,
                        765_000L,
                        500_000L,
                        265_000L,
                        200_000L,
                        Instant.parse("2026-10-23T08:00:00Z")
                );

        assertEquals(1_000_000L, view.capturedAmount());
        assertEquals(100_000L, view.refundedAmount());
        assertEquals(90_000L, view.commissionAmount());
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AdminFinanceSummaryView(
                        -1L,
                        0L,
                        0L,
                        0L,
                        0L,
                        0L,
                        0L,
                        0L,
                        0L,
                        Instant.parse("2026-10-23T08:00:00Z")
                )
        );
    }

    @Test
    void shouldRejectNullAsOf() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AdminFinanceSummaryView(
                        0L,
                        0L,
                        0L,
                        0L,
                        0L,
                        0L,
                        0L,
                        0L,
                        0L,
                        null
                )
        );
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
}