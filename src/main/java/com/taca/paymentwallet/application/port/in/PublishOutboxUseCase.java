package com.taca.paymentwallet.application.port.in;

public interface PublishOutboxUseCase {

    int publishNextBatch();
}