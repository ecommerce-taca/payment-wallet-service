package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.exception.AdminFinanceSummaryExportNotFoundException;
import com.taca.paymentwallet.application.export.AdminFinanceSummaryExportDownload;
import com.taca.paymentwallet.application.port.in.DownloadAdminFinanceSummaryExportUseCase;
import com.taca.paymentwallet.application.port.out.AdminFinanceSummaryExportDownloadPort;

import java.util.Objects;

public class DownloadAdminFinanceSummaryExportService
        implements DownloadAdminFinanceSummaryExportUseCase {

    private final AdminFinanceSummaryExportDownloadPort downloadPort;

    public DownloadAdminFinanceSummaryExportService(
            AdminFinanceSummaryExportDownloadPort downloadPort
    ) {
        this.downloadPort = Objects.requireNonNull(downloadPort);
    }

    @Override
    public AdminFinanceSummaryExportDownload execute(String token) {
        if (token == null || token.isBlank()) {
            throw new AdminFinanceSummaryExportNotFoundException();
        }

        return downloadPort.find(token.trim())
                .orElseThrow(AdminFinanceSummaryExportNotFoundException::new);
    }
}