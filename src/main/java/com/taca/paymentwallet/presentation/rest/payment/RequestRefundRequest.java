package com.taca.paymentwallet.presentation.rest.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RequestRefundRequest(
        @Positive long amount,

        @NotBlank
        @Size(max = 500)
        String reason
) {
}