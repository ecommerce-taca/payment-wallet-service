package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.command.CreatePaymentCommand;
import com.taca.paymentwallet.application.result.CreatePaymentResult;

public interface CreatePaymentUseCase {

    CreatePaymentResult execute(CreatePaymentCommand command);
}
