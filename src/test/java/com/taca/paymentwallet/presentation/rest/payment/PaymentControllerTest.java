package com.taca.paymentwallet.presentation.rest.payment;

import com.taca.paymentwallet.application.command.CreatePaymentCommand;
import com.taca.paymentwallet.application.command.ProcessVnpayWebhookCommand;
import com.taca.paymentwallet.application.exception.IdempotencyKeyReuseException;
import com.taca.paymentwallet.application.exception.InvalidVnpaySignatureException;
import com.taca.paymentwallet.application.exception.PaymentAmountMismatchException;
import com.taca.paymentwallet.application.exception.PaymentAttemptNotFoundException;
import com.taca.paymentwallet.application.port.in.CreatePaymentUseCase;
import com.taca.paymentwallet.application.port.in.ProcessVnpayWebhookUseCase;
import com.taca.paymentwallet.application.result.CreatePaymentResult;
import com.taca.paymentwallet.application.result.ProcessVnpayWebhookResult;
import com.taca.paymentwallet.application.result.WebhookProcessingAction;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.presentation.rest.GlobalRestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaymentControllerTest {

    private CreatePaymentUseCase createPaymentUseCase;

    private ProcessVnpayWebhookUseCase processVnpayWebhookUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        createPaymentUseCase =
                mock(
                        CreatePaymentUseCase.class
                );

        processVnpayWebhookUseCase =
                mock(
                        ProcessVnpayWebhookUseCase.class
                );

        PaymentController controller =
                new PaymentController(
                        createPaymentUseCase,
                        processVnpayWebhookUseCase
                );

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(
                                controller
                        )
                        .setControllerAdvice(
                                new GlobalRestExceptionHandler()
                        )
                        .build();
    }

    @Test
    void shouldCreateVnpayPayment() throws Exception {
        UUID checkoutGroupId =
                UUID.randomUUID();

        UUID buyerUserId =
                UUID.randomUUID();

        UUID orderId =
                UUID.randomUUID();

        UUID shopId =
                UUID.randomUUID();

        UUID paymentId =
                UUID.randomUUID();

        Instant expiresAt =
                Instant.parse(
                        "2026-09-27T08:15:00Z"
                );

        when(
                createPaymentUseCase.execute(
                        any()
                )
        ).thenReturn(
                new CreatePaymentResult(
                        paymentId,
                        checkoutGroupId,
                        "PENDING",
                        "VNPAY",
                        100_000,
                        "VND",
                        "https://sandbox.vnpay.vn/payment-url",
                        expiresAt
                )
        );

        String body =
                """
                {
                  "checkout_group_id": "%s",
                  "buyer_user_id": "%s",
                  "method": "VNPAY",
                  "amount": 100000,
                  "currency": "VND",
                  "orders": [
                    {
                      "order_id": "%s",
                      "shop_id": "%s",
                      "amount": 100000
                    }
                  ]
                }
                """.formatted(
                        checkoutGroupId,
                        buyerUserId,
                        orderId,
                        shopId
                );

        mockMvc.perform(
                        post(
                                "/api/v1/payments"
                        )
                                .header(
                                        "Idempotency-Key",
                                        "idem-001"
                                )
                                .header(
                                        "X-Request-ID",
                                        "req-001"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        body
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath(
                                "$.data.payment_id"
                        ).value(
                                paymentId.toString()
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.checkout_group_id"
                        ).value(
                                checkoutGroupId.toString()
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.status"
                        ).value(
                                "PENDING"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.method"
                        ).value(
                                "VNPAY"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.amount"
                        ).value(
                                100_000
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.currency"
                        ).value(
                                "VND"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.payment_url"
                        ).value(
                                "https://sandbox.vnpay.vn/payment-url"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.expires_at"
                        ).value(
                                expiresAt.toString()
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.meta.request_id"
                        ).value(
                                "req-001"
                        )
                );

        ArgumentCaptor<CreatePaymentCommand> captor =
                ArgumentCaptor.forClass(
                        CreatePaymentCommand.class
                );

        verify(
                createPaymentUseCase
        ).execute(
                captor.capture()
        );

        CreatePaymentCommand command =
                captor.getValue();

        assertEquals(
                checkoutGroupId,
                command.checkoutGroupId()
        );

        assertEquals(
                buyerUserId,
                command.buyerUserId()
        );

        assertEquals(
                "VNPAY",
                command.method()
        );

        assertEquals(
                100_000L,
                command.amount()
        );

        assertEquals(
                "VND",
                command.currency()
        );

        assertEquals(
                "idem-001",
                command.idempotencyKey()
        );

        assertEquals(
                1,
                command.orders().size()
        );
    }

    @Test
    void shouldRejectMissingIdempotencyKey()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/payments"
                        )
                                .header(
                                        "X-Request-ID",
                                        "req-001"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validBody()
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath(
                                "$.error.code"
                        ).value(
                                "MISSING_REQUIRED_HEADER"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.error.message"
                        ).value(
                                "Missing required request header: Idempotency-Key"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.meta.request_id"
                        ).value(
                                "req-001"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.error.code"
                        ).value(
                                "MISSING_REQUIRED_HEADER"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.meta.request_id"
                        ).isNotEmpty()
                );

        verifyNoInteractions(
                createPaymentUseCase
        );
    }

    @Test
    void shouldRejectMissingRequestId()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/payments"
                        )
                                .header(
                                        "Idempotency-Key",
                                        "idem-001"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validBody()
                                )
                )
                .andExpect(
                        status().isBadRequest()
                );

        verifyNoInteractions(
                createPaymentUseCase
        );
    }

    @Test
    void shouldRejectInvalidRequestBody()
            throws Exception {

        String body =
                """
                {
                  "method": "VNPAY",
                  "amount": 0,
                  "currency": "",
                  "orders": []
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/v1/payments"
                        )
                                .header(
                                        "Idempotency-Key",
                                        "idem-001"
                                )
                                .header(
                                        "X-Request-ID",
                                        "req-001"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        body
                                )
                )
                .andExpect(
                        status().isBadRequest()
                );

        verifyNoInteractions(
                createPaymentUseCase
        );
    }

    @Test
    void shouldCreateCodPaymentWithoutPaymentUrl()
            throws Exception {

        UUID checkoutGroupId =
                UUID.fromString(
                        "11111111-1111-1111-1111-111111111111"
                );

        UUID paymentId =
                UUID.randomUUID();

        when(
                createPaymentUseCase.execute(
                        any()
                )
        ).thenReturn(
                new CreatePaymentResult(
                        paymentId,
                        checkoutGroupId,
                        "PENDING_COD",
                        "COD",
                        100_000,
                        "VND",
                        null,
                        null
                )
        );

        String body =
                """
                {
                  "checkout_group_id":
                    "11111111-1111-1111-1111-111111111111",
                  "buyer_user_id":
                    "22222222-2222-2222-2222-222222222222",
                  "method": "COD",
                  "amount": 100000,
                  "currency": "VND",
                  "orders": [
                    {
                      "order_id":
                        "33333333-3333-3333-3333-333333333333",
                      "shop_id":
                        "44444444-4444-4444-4444-444444444444",
                      "amount": 100000
                    }
                  ]
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/v1/payments"
                        )
                                .header(
                                        "Idempotency-Key",
                                        "idem-002"
                                )
                                .header(
                                        "X-Request-ID",
                                        "req-002"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        body
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath(
                                "$.data.status"
                        ).value(
                                "PENDING_COD"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.payment_url"
                        ).doesNotExist()
                )
                .andExpect(
                        jsonPath(
                                "$.data.expires_at"
                        ).doesNotExist()
                );
    }

    @Test
    void shouldProcessSuccessfulVnpayWebhook()
            throws Exception {

        UUID paymentId =
                UUID.randomUUID();

        when(
                processVnpayWebhookUseCase.execute(
                        any()
                )
        ).thenReturn(
                new ProcessVnpayWebhookResult(
                        paymentId,
                        "SUCCESS",
                        WebhookProcessingAction.APPLIED
                )
        );

        String body =
                """
                {
                  "provider_event_id": "vnpay-event-001",
                  "provider_transaction_ref": "vnpay-txn-001",
                  "response_code": "00",
                  "transaction_status": "00",
                  "amount": 100000,
                  "currency": "VND",
                  "payload_hash":
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                  "signed_payload": {
                    "vnp_TxnRef": "vnpay-txn-001",
                    "vnp_Amount": "10000000",
                    "vnp_ResponseCode": "00",
                    "vnp_TransactionStatus": "00",
                    "vnp_SecureHash": "signed-value"
                  }
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/v1/payments/webhook"
                        )
                                .header(
                                        "X-Request-ID",
                                        "req-webhook-001"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        body
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.data.payment_id"
                        ).value(
                                paymentId.toString()
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.payment_status"
                        ).value(
                                "SUCCESS"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.action"
                        ).value(
                                "APPLIED"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.meta.request_id"
                        ).value(
                                "req-webhook-001"
                        )
                );

        ArgumentCaptor<ProcessVnpayWebhookCommand>
                captor =
                ArgumentCaptor.forClass(
                        ProcessVnpayWebhookCommand.class
                );

        verify(
                processVnpayWebhookUseCase
        ).execute(
                captor.capture()
        );

        ProcessVnpayWebhookCommand command =
                captor.getValue();

        assertEquals(
                "vnpay-event-001",
                command.providerEventId()
        );

        assertEquals(
                "vnpay-txn-001",
                command.providerTransactionRef()
        );

        assertEquals(
                "00",
                command.responseCode()
        );

        assertEquals(
                "00",
                command.transactionStatus()
        );

        assertEquals(
                100_000L,
                command.amount()
        );

        assertEquals(
                "VND",
                command.currency()
        );

        assertEquals(
                "signed-value",
                command.signedPayload()
                        .get(
                                "vnp_SecureHash"
                        )
        );
    }

    @Test
    void shouldAcknowledgeDuplicateVnpayWebhook()
            throws Exception {

        UUID paymentId =
                UUID.randomUUID();

        when(
                processVnpayWebhookUseCase.execute(
                        any()
                )
        ).thenReturn(
                new ProcessVnpayWebhookResult(
                        paymentId,
                        "SUCCESS",
                        WebhookProcessingAction.DUPLICATE
                )
        );

        mockMvc.perform(
                        post(
                                "/api/v1/payments/webhook"
                        )
                                .header(
                                        "X-Request-ID",
                                        "req-webhook-002"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validWebhookBody()
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.data.action"
                        ).value(
                                "DUPLICATE"
                        )
                );
    }

    @Test
    void shouldRejectInvalidVnpayWebhookBody()
            throws Exception {

        String body =
                """
                {
                  "provider_event_id": "",
                  "provider_transaction_ref": "",
                  "amount": 0,
                  "currency": "",
                  "signed_payload": {}
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/v1/payments/webhook"
                        )
                                .header(
                                        "X-Request-ID",
                                        "req-webhook-003"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        body
                                )
                )
                .andExpect(
                        status().isBadRequest()
                );

        verifyNoInteractions(
                processVnpayWebhookUseCase
        );
    }

    @Test
    void shouldReturnBadRequestWhenVnpaySignatureInvalid()
            throws Exception {

        when(
                processVnpayWebhookUseCase.execute(
                        any()
                )
        ).thenThrow(
                new InvalidVnpaySignatureException()
        );

        mockMvc.perform(
                        post(
                                "/api/v1/payments/webhook"
                        )
                                .header(
                                        "X-Request-ID",
                                        "req-webhook-invalid-signature"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validWebhookBody()
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath(
                                "$.error.code"
                        ).value(
                                "INVALID_VNPAY_SIGNATURE"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.error.message"
                        ).value(
                                "Invalid VNPAY signature"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.meta.request_id"
                        ).value(
                                "req-webhook-invalid-signature"
                        )
                );
    }

    @Test
    void shouldReturnConflictWhenWebhookAmountMismatch()
            throws Exception {

        UUID paymentId =
                UUID.randomUUID();

        when(
                processVnpayWebhookUseCase.execute(
                        any()
                )
        ).thenThrow(
                new PaymentAmountMismatchException(
                        new PaymentId(
                                paymentId
                        ),
                        Money.vnd(
                                100_000
                        ),
                        Money.vnd(
                                90_000
                        )
                )
        );

        mockMvc.perform(
                        post(
                                "/api/v1/payments/webhook"
                        )
                                .header(
                                        "X-Request-ID",
                                        "req-webhook-amount"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validWebhookBody()
                                )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath(
                                "$.error.code"
                        ).value(
                                "PAYMENT_AMOUNT_MISMATCH"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.meta.request_id"
                        ).value(
                                "req-webhook-amount"
                        )
                );
    }

    @Test
    void shouldReturnNotFoundWhenPaymentAttemptDoesNotExist()
            throws Exception {

        when(
                processVnpayWebhookUseCase.execute(
                        any()
                )
        ).thenThrow(
                new PaymentAttemptNotFoundException(
                        "VNPAY",
                        "vnpay-txn-001"
                )
        );

        mockMvc.perform(
                        post(
                                "/api/v1/payments/webhook"
                        )
                                .header(
                                        "X-Request-ID",
                                        "req-webhook-not-found"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validWebhookBody()
                                )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath(
                                "$.error.code"
                        ).value(
                                "PAYMENT_ATTEMPT_NOT_FOUND"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.meta.request_id"
                        ).value(
                                "req-webhook-not-found"
                        )
                );
    }

    @Test
    void shouldReturnConflictWhenIdempotencyKeyIsReused()
            throws Exception {

        when(
                createPaymentUseCase.execute(
                        any()
                )
        ).thenThrow(
                new IdempotencyKeyReuseException(
                        "idem-001"
                )
        );

        mockMvc.perform(
                        post(
                                "/api/v1/payments"
                        )
                                .header(
                                        "Idempotency-Key",
                                        "idem-001"
                                )
                                .header(
                                        "X-Request-ID",
                                        "req-idempotency-conflict"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validBody()
                                )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath(
                                "$.error.code"
                        ).value(
                                "IDEMPOTENCY_KEY_REUSED"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.meta.request_id"
                        ).value(
                                "req-idempotency-conflict"
                        )
                );
    }

    @Test
    void shouldHideUnexpectedInternalErrorDetails()
            throws Exception {

        when(
                createPaymentUseCase.execute(
                        any()
                )
        ).thenThrow(
                new RuntimeException(
                        "jdbc:mysql://secret-db:3306/payment password=secret"
                )
        );

        mockMvc.perform(
                        post(
                                "/api/v1/payments"
                        )
                                .header(
                                        "Idempotency-Key",
                                        "idem-001"
                                )
                                .header(
                                        "X-Request-ID",
                                        "req-internal-error"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validBody()
                                )
                )
                .andExpect(
                        status().isInternalServerError()
                )
                .andExpect(
                        jsonPath(
                                "$.error.code"
                        ).value(
                                "INTERNAL_ERROR"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.error.message"
                        ).value(
                                "Internal server error"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.meta.request_id"
                        ).value(
                                "req-internal-error"
                        )
                );
    }

    private String validBody() {
        return """
                {
                  "checkout_group_id":
                    "11111111-1111-1111-1111-111111111111",
                  "buyer_user_id":
                    "22222222-2222-2222-2222-222222222222",
                  "method": "VNPAY",
                  "amount": 100000,
                  "currency": "VND",
                  "orders": [
                    {
                      "order_id":
                        "33333333-3333-3333-3333-333333333333",
                      "shop_id":
                        "44444444-4444-4444-4444-444444444444",
                      "amount": 100000
                    }
                  ]
                }
                """;
    }

    private String validWebhookBody() {
        return """
            {
              "provider_event_id": "vnpay-event-001",
              "provider_transaction_ref": "vnpay-txn-001",
              "response_code": "00",
              "transaction_status": "00",
              "amount": 100000,
              "currency": "VND",
              "payload_hash":
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
              "signed_payload": {
                "vnp_TxnRef": "vnpay-txn-001",
                "vnp_Amount": "10000000",
                "vnp_ResponseCode": "00",
                "vnp_TransactionStatus": "00",
                "vnp_SecureHash": "signed-value"
              }
            }
            """;
    }
}