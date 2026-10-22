package com.taca.paymentwallet.application.result;

import java.time.Instant;
import java.util.UUID;

public record GetAdminPaymentReconciliationItemResult(
        UUID paymentId,
        String provider,
        String localStatus,
        String providerStatus,
        long amount,
        boolean matched,
        Instant checkedAt
) {
}