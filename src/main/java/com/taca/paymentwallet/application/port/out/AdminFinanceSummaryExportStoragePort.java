package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.export.AdminFinanceSummaryExportFile;
import com.taca.paymentwallet.application.export.AdminFinanceSummaryExportLocation;

import java.time.Duration;

public interface AdminFinanceSummaryExportStoragePort {

    AdminFinanceSummaryExportLocation store(
            AdminFinanceSummaryExportFile file,
            Duration urlTtl
    );
}