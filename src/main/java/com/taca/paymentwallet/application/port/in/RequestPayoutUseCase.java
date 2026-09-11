package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.command.RequestPayoutCommand;
import com.taca.paymentwallet.application.result.RequestPayoutResult;

public interface RequestPayoutUseCase {

    RequestPayoutResult execute(RequestPayoutCommand command);
}
