package com.taca.paymentwallet.application.result;

import java.time.Instant;
import java.util.UUID;

public record CreateAdminFeeConfigResult(
        UUID id,
        String scope,
        UUID categoryId,
        int rateBps,
        Instant effectiveFrom,
        String note,
        UUID createdBy,
        Instant createdAt
) {
}