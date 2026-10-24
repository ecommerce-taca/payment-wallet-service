package com.taca.paymentwallet.application.port.out;

import java.time.Instant;
import java.util.UUID;

public record AdminTaxConfigWriteModel(
        String scope,
        UUID categoryId,
        int rateBps,
        Instant effectiveFrom,
        String note,
        UUID createdBy
) {
}