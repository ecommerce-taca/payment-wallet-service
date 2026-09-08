package com.taca.paymentwallet.application.result;

import java.util.UUID;

public record CreatePaymentResult(
        UUID paymentId,
        String status,
        String paymentUrl
) {
}
