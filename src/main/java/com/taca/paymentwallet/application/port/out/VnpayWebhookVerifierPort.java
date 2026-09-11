package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.command.ProcessVnpayWebhookCommand;

public interface VnpayWebhookVerifierPort {

    void verify(ProcessVnpayWebhookCommand command);
}
