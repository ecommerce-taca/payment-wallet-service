package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.command.RunSettlementCommand;
import com.taca.paymentwallet.application.result.RunSettlementResult;

public interface RunSettlementUseCase {

    RunSettlementResult execute(RunSettlementCommand command);
}