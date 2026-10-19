package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.port.in.GetAdminTaxConfigsUseCase;
import com.taca.paymentwallet.application.port.out.AdminTaxConfigReadPort;
import com.taca.paymentwallet.application.port.out.AdminTaxConfigView;
import com.taca.paymentwallet.application.result.GetAdminTaxConfigResult;

import java.util.List;
import java.util.Objects;

public class GetAdminTaxConfigsService implements GetAdminTaxConfigsUseCase {

    private final AdminTaxConfigReadPort readPort;

    public GetAdminTaxConfigsService(AdminTaxConfigReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort);
    }

    @Override
    public List<GetAdminTaxConfigResult> execute() {
        return readPort.findAll().stream().map(this::toResult).toList();
    }

    private GetAdminTaxConfigResult toResult(AdminTaxConfigView view) {
        return new GetAdminTaxConfigResult(
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