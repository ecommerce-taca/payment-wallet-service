package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.command.ProcessVnpayWebhookCommand;
import com.taca.paymentwallet.application.result.ProcessVnpayWebhookResult;

public interface ProcessVnpayWebhookUseCase {

    ProcessVnpayWebhookResult execute(ProcessVnpayWebhookCommand command);
}