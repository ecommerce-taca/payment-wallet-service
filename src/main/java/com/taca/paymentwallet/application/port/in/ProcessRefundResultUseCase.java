package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.command.ProcessRefundResultCommand;
import com.taca.paymentwallet.application.result.ProcessRefundResult;

public interface ProcessRefundResultUseCase {

    ProcessRefundResult execute(ProcessRefundResultCommand command);
}