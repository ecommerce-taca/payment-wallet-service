package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.exception.PaymentNotFoundException;
import com.taca.paymentwallet.application.port.in.GetPaymentUseCase;
import com.taca.paymentwallet.application.port.out.PaymentRepositoryPort;
import com.taca.paymentwallet.application.query.GetPaymentQuery;
import com.taca.paymentwallet.application.result.GetPaymentOrderResult;
import com.taca.paymentwallet.application.result.GetPaymentResult;
import com.taca.paymentwallet.application.security.PaymentVisibilityPolicy;
import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.valueobject.PaymentId;

import java.util.Objects;

public class GetPaymentService implements GetPaymentUseCase {

    private final PaymentRepositoryPort paymentRepository;

    private final PaymentVisibilityPolicy paymentVisibilityPolicy;

    public GetPaymentService(
            PaymentRepositoryPort paymentRepository,
            PaymentVisibilityPolicy paymentVisibilityPolicy
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository);
        this.paymentVisibilityPolicy = Objects.requireNonNull(paymentVisibilityPolicy);
    }

    @Override
    public GetPaymentResult execute(GetPaymentQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        PaymentId paymentId = new PaymentId(query.paymentId());

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        GetPaymentResult result = toResult(payment);

        paymentVisibilityPolicy.requireCanView(result);

        return result;
    }

    private GetPaymentResult toResult(Payment payment) {
        return new GetPaymentResult(
                payment.id().value(),
                payment.checkoutGroupId().value(),
                payment.buyerUserId().value(),
                payment.status().name(),
                payment.method().name(),
                payment.amount().amount(),
                payment.amount().currency(),
                payment.capturedAmount().amount(),
                payment.refundedAmount().amount(),
                payment.expiresAt(),
                payment.paidAt(),
                payment.orders()
                        .stream()
                        .map(this::toOrderResult)
                        .toList()
        );
    }

    private GetPaymentOrderResult toOrderResult(PaymentOrder order) {
        return new GetPaymentOrderResult(
                order.orderId().value(),
                order.shopId().value(),
                order.merchandiseAmount().amount(),
                order.shippingFee().amount(),
                order.totalAmount().amount()
        );
    }
}