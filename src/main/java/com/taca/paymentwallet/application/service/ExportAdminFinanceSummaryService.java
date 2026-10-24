package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.export.AdminFinanceSummaryCsvRenderer;
import com.taca.paymentwallet.application.export.AdminFinanceSummaryExportFile;
import com.taca.paymentwallet.application.export.AdminFinanceSummaryExportLocation;
import com.taca.paymentwallet.application.port.in.ExportAdminFinanceSummaryUseCase;
import com.taca.paymentwallet.application.port.out.AdminFinanceSummaryExportStoragePort;
import com.taca.paymentwallet.application.port.out.AdminFinanceSummaryReadPort;
import com.taca.paymentwallet.application.port.out.AdminFinanceSummaryView;
import com.taca.paymentwallet.application.result.ExportAdminFinanceSummaryResult;

import java.time.Duration;
import java.util.Objects;

public class ExportAdminFinanceSummaryService
        implements ExportAdminFinanceSummaryUseCase {

    public static final Duration EXPORT_URL_TTL = Duration.ofMinutes(30);

    private final AdminFinanceSummaryReadPort readPort;
    private final AdminFinanceSummaryExportStoragePort storagePort;
    private final AdminFinanceSummaryCsvRenderer csvRenderer;

    public ExportAdminFinanceSummaryService(
            AdminFinanceSummaryReadPort readPort,
            AdminFinanceSummaryExportStoragePort storagePort,
            AdminFinanceSummaryCsvRenderer csvRenderer
    ) {
        this.readPort = Objects.requireNonNull(readPort);
        this.storagePort = Objects.requireNonNull(storagePort);
        this.csvRenderer = Objects.requireNonNull(csvRenderer);
    }

    @Override
    public ExportAdminFinanceSummaryResult execute() {
        AdminFinanceSummaryView summary = readPort.getSummary();
        AdminFinanceSummaryExportFile file = csvRenderer.render(summary);
        AdminFinanceSummaryExportLocation location =
                storagePort.store(file, EXPORT_URL_TTL);

        return new ExportAdminFinanceSummaryResult(
                location.downloadUrl(),
                location.expiresAt()
        );
    }
}