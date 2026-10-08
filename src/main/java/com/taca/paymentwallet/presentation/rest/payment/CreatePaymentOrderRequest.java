package com.taca.paymentwallet.presentation.rest.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record CreatePaymentOrderRequest(
        @JsonProperty("order_id")
        @NotNull
        UUID orderId,

        @JsonProperty("shop_id")
        @NotNull
        UUID shopId,

        @Positive
        long amount,

        @JsonProperty("shipping_fee")
        @PositiveOrZero
        long shippingFee
) {
}