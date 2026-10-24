package com.taca.paymentwallet.application.exception;

public class AdminFinanceSummaryExportNotFoundException
        extends ApplicationException {

    public AdminFinanceSummaryExportNotFoundException() {
        super("Finance summary export not found or expired");
    }
}