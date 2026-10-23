package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.export.AdminFinanceSummaryCsvRenderer;
import com.taca.paymentwallet.application.export.AdminFinanceSummaryExportFile;
import com.taca.paymentwallet.application.export.AdminFinanceSummaryExportLocation;
import com.taca.paymentwallet.application.port.out.AdminFinanceSummaryExportStoragePort;
import com.taca.paymentwallet.application.port.out.AdminFinanceSummaryReadPort;
import com.taca.paymentwallet.application.port.out.AdminFinanceSummaryView;
import com.taca.paymentwallet.application.result.ExportAdminFinanceSummaryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ExportAdminFinanceSummaryServiceTest {

    private AdminFinanceSummaryReadPort readPort;
    private AdminFinanceSummaryExportStoragePort storagePort;
    private ExportAdminFinanceSummaryService service;

    @BeforeEach
    void setUp() {
        readPort = mock(AdminFinanceSummaryReadPort.class);
        storagePort = mock(AdminFinanceSummaryExportStoragePort.class);
        service = new ExportAdminFinanceSummaryService(
                readPort,
                storagePort,
                new AdminFinanceSummaryCsvRenderer()
        );
    }

    @Test
    void shouldExportFinanceSummaryWithThirtyMinuteUrl() {
        AdminFinanceSummaryView summary = summary();
        Instant expiresAt = Instant.parse("2026-10-23T16:00:00Z");

        when(readPort.getSummary()).thenReturn(summary);
        when(storagePort.store(any(), eq(Duration.ofMinutes(30))))
                .thenReturn(new AdminFinanceSummaryExportLocation(
                        "https://storage.example/export.csv",
                        expiresAt
                ));

        ExportAdminFinanceSummaryResult result = service.execute();

        assertThat(result.downloadUrl())
                .isEqualTo("https://storage.example/export.csv");
        assertThat(result.expiresAt()).isEqualTo(expiresAt);

        verify(readPort).getSummary();
        verify(storagePort).store(any(), eq(Duration.ofMinutes(30)));
    }

    @Test
    void shouldStoreCsvWithExpectedSummaryContent() {
        when(readPort.getSummary()).thenReturn(summary());
        when(storagePort.store(any(), any()))
                .thenReturn(new AdminFinanceSummaryExportLocation(
                        "https://storage.example/export.csv",
                        Instant.parse("2026-10-23T16:00:00Z")
                ));

        service.execute();

        ArgumentCaptor<AdminFinanceSummaryExportFile> captor =
                ArgumentCaptor.forClass(AdminFinanceSummaryExportFile.class);

        verify(storagePort).store(
                captor.capture(),
                eq(Duration.ofMinutes(30))
        );

        AdminFinanceSummaryExportFile file = captor.getValue();

        assertThat(file.fileName())
                .isEqualTo("admin-finance-summary-2026-10-23T15-30-00Z.csv");
        assertThat(file.contentType())
                .isEqualTo("text/csv; charset=UTF-8");
        assertThat(file.content()).isNotEmpty();
    }

    private AdminFinanceSummaryView summary() {
        return new AdminFinanceSummaryView(
                1_000_000,
                100_000,
                900_000,
                70_000,
                30_000,
                800_000,
                500_000,
                300_000,
                200_000,
                Instant.parse("2026-10-23T15:30:00Z")
        );
    }
}