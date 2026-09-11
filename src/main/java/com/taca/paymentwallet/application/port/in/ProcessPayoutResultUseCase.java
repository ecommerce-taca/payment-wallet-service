package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.command.ProcessPayoutResultCommand;
import com.taca.paymentwallet.application.result.ProcessPayoutResult;

public interface ProcessPayoutResultUseCase {

    ProcessPayoutResult execute(ProcessPayoutResultCommand command);
}
