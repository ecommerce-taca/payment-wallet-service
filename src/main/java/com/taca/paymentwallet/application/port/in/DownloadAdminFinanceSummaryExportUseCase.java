package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.export.AdminFinanceSummaryExportDownload;

public interface DownloadAdminFinanceSummaryExportUseCase {

    AdminFinanceSummaryExportDownload execute(String token);
}