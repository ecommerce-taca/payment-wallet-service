package com.taca.paymentwallet.application.result;

import java.util.UUID;

public record RetryAdminSettlementResult(UUID batchId, String status, int retriedItemCount) {
}