package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.port.in.GetAdminFeeConfigsUseCase;
import com.taca.paymentwallet.application.port.out.AdminFeeConfigReadPort;
import com.taca.paymentwallet.application.port.out.AdminFeeConfigView;
import com.taca.paymentwallet.application.result.GetAdminFeeConfigResult;

import java.util.List;
import java.util.Objects;

public class GetAdminFeeConfigsService implements GetAdminFeeConfigsUseCase {

    private final AdminFeeConfigReadPort readPort;

    public GetAdminFeeConfigsService(AdminFeeConfigReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort);
    }

    @Override
    public List<GetAdminFeeConfigResult> execute() {
        return readPort.findAll().stream().map(this::toResult).toList();
    }

    private GetAdminFeeConfigResult toResult(AdminFeeConfigView view) {
        return new GetAdminFeeConfigResult(
                view.id(),
                view.scope(),
                view.categoryId(),
                view.rateBps(),
                view.effectiveFrom(),
                view.note(),
                view.createdBy(),
                view.createdAt()
        );
    }
}