package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.command.RetryAdminSettlementCommand;
import com.taca.paymentwallet.application.result.RetryAdminSettlementResult;

public interface RetryAdminSettlementUseCase {

    RetryAdminSettlementResult execute(RetryAdminSettlementCommand command);
}