package com.taca.paymentwallet.presentation.rest.payment;

import com.taca.paymentwallet.application.command.CreatePaymentCommand;
import com.taca.paymentwallet.application.command.CreatePaymentOrderCommand;
import com.taca.paymentwallet.application.command.ProcessVnpayWebhookCommand;
import com.taca.paymentwallet.application.exception.InvalidPaymentRequestException;
import com.taca.paymentwallet.application.port.in.CreatePaymentUseCase;
import com.taca.paymentwallet.application.port.in.ProcessVnpayWebhookUseCase;
import com.taca.paymentwallet.application.result.CreatePaymentResult;
import com.taca.paymentwallet.application.result.ProcessVnpayWebhookResult;
import com.taca.paymentwallet.application.security.InternalCallerPolicy;
import com.taca.paymentwallet.presentation.rest.ApiMeta;
import com.taca.paymentwallet.presentation.rest.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final CreatePaymentUseCase createPaymentUseCase;

    private final ProcessVnpayWebhookUseCase processVnpayWebhookUseCase;

    private final InternalCallerPolicy internalCallerPolicy;

    public PaymentController(
            CreatePaymentUseCase createPaymentUseCase,
            ProcessVnpayWebhookUseCase processVnpayWebhookUseCase,
            InternalCallerPolicy internalCallerPolicy
    ) {
        this.createPaymentUseCase = Objects.requireNonNull(createPaymentUseCase);
        this.processVnpayWebhookUseCase = Objects.requireNonNull(processVnpayWebhookUseCase);
        this.internalCallerPolicy = Objects.requireNonNull(internalCallerPolicy);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreatePaymentData>> createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-Request-ID") String requestId,
            @Valid @RequestBody CreatePaymentRequest request,
            HttpServletRequest httpServletRequest
    ) {
        requireHeaderValue(idempotencyKey, "Idempotency-Key");
        requireHeaderValue(requestId, "X-Request-ID");

        internalCallerPolicy.requireOrderCommerce();

        CreatePaymentCommand command = toCommand(
                request,
                idempotencyKey,
                httpServletRequest.getRemoteAddr()
        );

        CreatePaymentResult result = createPaymentUseCase.execute(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(
                        toData(result),
                        new ApiMeta(requestId.trim())
                )
        );
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<VnpayWebhookData>>
    processVnpayWebhook(
            @RequestHeader("X-Request-ID")
            String requestId,

            @Valid
            @RequestBody
            VnpayWebhookRequest request
    ) {
        ProcessVnpayWebhookCommand command =
                new ProcessVnpayWebhookCommand(
                        request.providerEventId(),
                        request.providerTransactionRef(),
                        request.responseCode(),
                        request.transactionStatus(),
                        request.amount(),
                        request.currency(),
                        request.payloadHash(),
                        request.signedPayload()
                );

        ProcessVnpayWebhookResult result =
                processVnpayWebhookUseCase.execute(
                        command
                );

        ApiResponse<VnpayWebhookData> response =
                new ApiResponse<>(
                        new VnpayWebhookData(
                                result.paymentId(),
                                result.paymentStatus(),
                                result.action().name()
                        ),
                        new ApiMeta(
                                requestId
                        )
                );

        return ResponseEntity.ok(
                response
        );
    }

    private void requireHeaderValue(String value, String headerName) {
        if (value == null || value.isBlank()) {
            throw new InvalidPaymentRequestException(headerName + " must not be blank");
        }
    }

    private CreatePaymentCommand toCommand(
            CreatePaymentRequest request,
            String idempotencyKey,
            String clientIp
    ) {
        return new CreatePaymentCommand(
                request.checkoutGroupId(),
                request.buyerUserId(),
                request.method(),
                request.amount(),
                request.currency(),
                idempotencyKey,
                request.orders()
                        .stream()
                        .map(
                                order ->
                                        new CreatePaymentOrderCommand(
                                                order.orderId(),
                                                order.shopId(),
                                                order.amount(),
                                                order.shippingFee()
                                        )
                        )
                        .toList(),
                clientIp
        );
    }

    private CreatePaymentData toData(
            CreatePaymentResult result
    ) {
        return new CreatePaymentData(
                result.paymentId(),
                result.checkoutGroupId(),
                result.status(),
                result.method(),
                result.amount(),
                result.currency(),
                result.paymentUrl(),
                result.expiresAt()
        );
    }
}