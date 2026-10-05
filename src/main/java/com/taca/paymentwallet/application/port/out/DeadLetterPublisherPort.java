package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.outbox.OutboxDeadLetter;

public interface DeadLetterPublisherPort {

    void publish(OutboxDeadLetter deadLetter);
}