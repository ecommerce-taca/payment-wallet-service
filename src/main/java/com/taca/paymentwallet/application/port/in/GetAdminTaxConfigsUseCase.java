package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.result.GetAdminTaxConfigResult;

import java.util.List;

public interface GetAdminTaxConfigsUseCase {

    List<GetAdminTaxConfigResult> execute();
}