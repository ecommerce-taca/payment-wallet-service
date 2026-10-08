package com.taca.paymentwallet.presentation.rest.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record GetPaymentOrderData(
        @JsonProperty("order_id")
        UUID orderId,

        @JsonProperty("shop_id")
        UUID shopId,

        @JsonProperty("merchandise_amount")
        long merchandiseAmount,

        @JsonProperty("shipping_fee")
        long shippingFee,

        long amount
) {
}