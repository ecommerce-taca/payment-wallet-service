package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.command.CreateAdminTaxConfigCommand;
import com.taca.paymentwallet.application.result.CreateAdminTaxConfigResult;

public interface CreateAdminTaxConfigUseCase {

    CreateAdminTaxConfigResult execute(CreateAdminTaxConfigCommand command);
}