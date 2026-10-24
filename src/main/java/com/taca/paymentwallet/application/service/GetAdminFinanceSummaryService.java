package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.port.in.GetAdminFinanceSummaryUseCase;
import com.taca.paymentwallet.application.port.out.AdminFinanceSummaryReadPort;
import com.taca.paymentwallet.application.port.out.AdminFinanceSummaryView;
import com.taca.paymentwallet.application.result.GetAdminFinanceSummaryResult;

import java.util.Objects;

public class GetAdminFinanceSummaryService
        implements GetAdminFinanceSummaryUseCase {

    private final AdminFinanceSummaryReadPort readPort;

    public GetAdminFinanceSummaryService(
            AdminFinanceSummaryReadPort readPort
    ) {
        this.readPort = Objects.requireNonNull(readPort);
    }

    @Override
    public GetAdminFinanceSummaryResult execute() {
        AdminFinanceSummaryView view =
                readPort.getSummary();

        return new GetAdminFinanceSummaryResult(
                view.capturedAmount(),
                view.refundedAmount(),
                view.grossAmount(),
                view.commissionAmount(),
                view.taxAmount(),
                view.sellerNetAmount(),
                view.availableBalance(),
                view.pendingBalance(),
                view.completedPayoutAmount(),
                view.asOf()
        );
    }
}