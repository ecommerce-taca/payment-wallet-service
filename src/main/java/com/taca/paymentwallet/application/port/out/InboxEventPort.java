package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.inbox.InboxEvent;
import com.taca.paymentwallet.application.inbox.InboxEventKey;

import java.time.Instant;

public interface InboxEventPort {

    boolean recordIfAbsent(InboxEvent event);

    void markProcessed(InboxEventKey key, Instant processedAt);
}