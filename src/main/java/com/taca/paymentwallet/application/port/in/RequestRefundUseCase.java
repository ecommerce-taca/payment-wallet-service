package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.command.RequestRefundCommand;
import com.taca.paymentwallet.application.result.RequestRefundResult;

public interface RequestRefundUseCase {

    RequestRefundResult execute(RequestRefundCommand command);
}