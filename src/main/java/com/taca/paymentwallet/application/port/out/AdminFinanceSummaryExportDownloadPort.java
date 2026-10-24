package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.export.AdminFinanceSummaryExportDownload;

import java.util.Optional;

public interface AdminFinanceSummaryExportDownloadPort {

    Optional<AdminFinanceSummaryExportDownload> find(String token);
}