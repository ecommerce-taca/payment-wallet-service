package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.command.CreateAdminFeeConfigCommand;
import com.taca.paymentwallet.application.result.CreateAdminFeeConfigResult;

public interface CreateAdminFeeConfigUseCase {

    CreateAdminFeeConfigResult execute(CreateAdminFeeConfigCommand command);
}