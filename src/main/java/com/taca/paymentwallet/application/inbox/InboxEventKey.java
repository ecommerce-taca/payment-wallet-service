package com.taca.paymentwallet.application.inbox;

public record InboxEventKey(
        String consumerName,
        String source,
        String eventId
) {

    public InboxEventKey {
        if (consumerName == null || consumerName.isBlank()) {
            throw new IllegalArgumentException("consumerName must not be blank");
        }

        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source must not be blank");
        }

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }

        consumerName = consumerName.trim();
        source = source.trim();
        eventId = eventId.trim();
    }
}
