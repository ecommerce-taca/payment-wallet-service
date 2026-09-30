package com.taca.paymentwallet.application.exception;

import com.taca.paymentwallet.application.inbox.InboxEventKey;

public class InboxEventNotFoundException extends ApplicationException {

    public InboxEventNotFoundException(InboxEventKey key) {
        super(
                "inbox event not found: "
                        + key.consumerName()
                        + "/"
                        + key.source()
                        + "/"
                        + key.eventId()
        );
    }
}