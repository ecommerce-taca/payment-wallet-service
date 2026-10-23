package com.taca.paymentwallet.application.command;

import java.util.UUID;

public record RetryAdminSettlementCommand(UUID batchId, UUID actorUserId) {

    public RetryAdminSettlementCommand {
        if (batchId == null) throw new IllegalArgumentException("batchId must not be null");
        if (actorUserId == null) throw new IllegalArgumentException("actorUserId must not be null");
    }
}