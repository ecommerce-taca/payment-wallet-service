package com.taca.paymentwallet.presentation.rest.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record CreatePaymentRequest(
        @JsonProperty("checkout_group_id")
        @NotNull
        UUID checkoutGroupId,

        @JsonProperty("buyer_user_id")
        @NotNull
        UUID buyerUserId,

        @NotBlank
        @Pattern(regexp = "VNPAY|COD")
        String method,

        @Positive
        long amount,

        @NotBlank
        @Pattern(regexp = "VND")
        String currency,

        @NotEmpty
        List<@Valid CreatePaymentOrderRequest> orders
) {
}