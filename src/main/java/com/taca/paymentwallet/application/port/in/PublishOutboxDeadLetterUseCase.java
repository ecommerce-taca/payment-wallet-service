package com.taca.paymentwallet.application.port.in;

public interface PublishOutboxDeadLetterUseCase {

    int publishNextBatch();
}