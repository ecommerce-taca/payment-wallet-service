package com.taca.paymentwallet.application.command;

public record ProcessInboxEventCommand(
        String consumerName,
        String source,
        String eventId,
        String eventType,
        String payloadHash
) {

    public ProcessInboxEventCommand {
        if (consumerName == null || consumerName.isBlank()) {
            throw new IllegalArgumentException("consumerName must not be blank");
        }

        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source must not be blank");
        }

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }

        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType must not be blank");
        }

        if (payloadHash == null || payloadHash.isBlank()) {
            throw new IllegalArgumentException("payloadHash must not be blank");
        }

        consumerName = consumerName.trim();
        source = source.trim();
        eventId = eventId.trim();
        eventType = eventType.trim();
        payloadHash = payloadHash.trim();
    }
}
