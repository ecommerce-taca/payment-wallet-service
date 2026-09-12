package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.command.ProcessCodPaymentCommand;
import com.taca.paymentwallet.application.result.ProcessCodPaymentResult;

public interface ProcessCodPaymentUseCase {

    ProcessCodPaymentResult execute(ProcessCodPaymentCommand command);
}