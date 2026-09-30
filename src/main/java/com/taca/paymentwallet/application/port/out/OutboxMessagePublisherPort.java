package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.outbox.OutboxMessage;

public interface OutboxMessagePublisherPort {

    boolean supports(OutboxMessage message);

    void publish(OutboxMessage message);
}