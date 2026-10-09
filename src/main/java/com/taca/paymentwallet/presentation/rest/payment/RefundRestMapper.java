package com.taca.paymentwallet.presentation.rest.payment;

import com.taca.paymentwallet.application.command.RequestRefundCommand;
import com.taca.paymentwallet.application.result.RequestRefundResult;

import java.util.Objects;
import java.util.UUID;

class RefundRestMapper {

    RequestRefundCommand toCommand(
            UUID paymentId,
            RequestRefundRequest request,
            String idempotencyKey
    ) {
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        return new RequestRefundCommand(
                paymentId,
                request.amount(),
                "VND",
                request.reason(),
                idempotencyKey
        );
    }

    RequestRefundData toData(
            RequestRefundResult result
    ) {
        Objects.requireNonNull(result, "result must not be null");

        return new RequestRefundData(
                result.refundId(),
                result.paymentId(),
                result.amount(),
                result.currency(),
                result.status()
        );
    }
}