package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.command.CreatePaymentCommand;
import com.taca.paymentwallet.application.command.RequestRefundCommand;

public interface RequestHashPort {

    String hash(CreatePaymentCommand command);

    String hash(RequestRefundCommand command);
}
