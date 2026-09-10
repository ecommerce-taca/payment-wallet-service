package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.command.CreatePaymentCommand;

public interface RequestHashPort {

    String hash(CreatePaymentCommand command);
}
