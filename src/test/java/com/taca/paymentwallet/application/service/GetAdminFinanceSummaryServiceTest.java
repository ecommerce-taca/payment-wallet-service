package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.port.out.AdminFinanceSummaryReadPort;
import com.taca.paymentwallet.application.port.out.AdminFinanceSummaryView;
import com.taca.paymentwallet.application.result.GetAdminFinanceSummaryResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GetAdminFinanceSummaryServiceTest {

    @Test
    void shouldReturnFinanceSummary() {
        AdminFinanceSummaryReadPort readPort =
                mock(AdminFinanceSummaryReadPort.class);

        Instant asOf =
                Instant.parse("2026-10-23T10:00:00Z");

        when(readPort.getSummary()).thenReturn(
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
                        asOf
                )
        );

        GetAdminFinanceSummaryService service =
                new GetAdminFinanceSummaryService(readPort);

        GetAdminFinanceSummaryResult result =
                service.execute();

        assertThat(result.capturedAmount())
                .isEqualTo(1_000_000L);

        assertThat(result.refundedAmount())
                .isEqualTo(100_000L);

        assertThat(result.grossAmount())
                .isEqualTo(900_000L);

        assertThat(result.commissionAmount())
                .isEqualTo(90_000L);

        assertThat(result.taxAmount())
                .isEqualTo(45_000L);

        assertThat(result.sellerNetAmount())
                .isEqualTo(765_000L);

        assertThat(result.availableBalance())
                .isEqualTo(500_000L);

        assertThat(result.pendingBalance())
                .isEqualTo(265_000L);

        assertThat(result.completedPayoutAmount())
                .isEqualTo(200_000L);

        assertThat(result.asOf())
                .isEqualTo(asOf);

        verify(readPort).getSummary();
    }
}