package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.port.in.GetAdminSettlementsUseCase;
import com.taca.paymentwallet.application.port.out.AdminSettlementBatchView;
import com.taca.paymentwallet.application.port.out.AdminSettlementReadPort;
import com.taca.paymentwallet.application.result.GetAdminSettlementBatchResult;

import java.util.List;
import java.util.Objects;

public class GetAdminSettlementsService implements GetAdminSettlementsUseCase {

    private final AdminSettlementReadPort readPort;

    public GetAdminSettlementsService(AdminSettlementReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort);
    }

    @Override
    public List<GetAdminSettlementBatchResult> execute() {
        return readPort.findAll().stream().map(this::toResult).toList();
    }

    private GetAdminSettlementBatchResult toResult(AdminSettlementBatchView view) {
        return new GetAdminSettlementBatchResult(
                view.id(),
                view.periodStart(),
                view.periodEnd(),
                view.status(),
                view.shopCount(),
                view.totalGross(),
                view.totalCommission(),
                view.totalTax(),
                view.totalNet(),
                view.totalReleased(),
                view.totalHeld(),
                view.createdAt(),
                view.closedAt(),
                view.lastError()
        );
    }
}