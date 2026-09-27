package com.taca.paymentwallet.presentation.rest.payment;

import com.taca.paymentwallet.application.command.CreatePaymentCommand;
import com.taca.paymentwallet.application.port.in.CreatePaymentUseCase;
import com.taca.paymentwallet.application.result.CreatePaymentResult;
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

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        createPaymentUseCase =
                mock(
                        CreatePaymentUseCase.class
                );

        PaymentController controller =
                new PaymentController(
                        createPaymentUseCase
                );

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(
                                controller
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
}