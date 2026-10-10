package com.taca.paymentwallet.presentation.rest;

import com.taca.paymentwallet.application.exception.RefundAmountMismatchException;
import com.taca.paymentwallet.application.exception.RefundNotFoundException;
import com.taca.paymentwallet.domain.payment.InvalidPaymentStateException;
import com.taca.paymentwallet.domain.payment.PaymentStatus;
import com.taca.paymentwallet.domain.refund.RefundLimitExceededException;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.RefundId;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalRestExceptionHandlerRefundTest {

    private final GlobalRestExceptionHandler handler =
            new GlobalRestExceptionHandler();

    @Test
    void shouldMapRefundLimitExceededToConflict() {
        HttpServletRequest request = request("req-refund-limit");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleRefundLimitExceeded(
                        new RefundLimitExceededException(
                                Money.vnd(100_000),
                                Money.vnd(150_000)
                        ),
                        request
                );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(
                "REFUND_AMOUNT_INVALID",
                response.getBody().error().code()
        );
        assertEquals(
                "req-refund-limit",
                response.getBody().meta().requestId()
        );
    }

    @Test
    void shouldMapInvalidPaymentStateToConflict() {
        HttpServletRequest request = request("req-refund-state");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleInvalidPaymentState(
                        new InvalidPaymentStateException(
                                PaymentStatus.PENDING,
                                "request refund"
                        ),
                        request
                );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(
                "REFUND_STATE_INVALID",
                response.getBody().error().code()
        );
    }

    @Test
    void shouldMapRefundNotFoundToNotFound() {
        HttpServletRequest request = request("req-refund-not-found");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleRefundNotFound(
                        new RefundNotFoundException(
                                new RefundId(UUID.randomUUID())
                        ),
                        request
                );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(
                "REFUND_NOT_FOUND",
                response.getBody().error().code()
        );
    }

    @Test
    void shouldMapRefundAmountMismatchToConflict() {
        HttpServletRequest request = request("req-refund-mismatch");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleRefundAmountMismatch(
                        new RefundAmountMismatchException(
                                new RefundId(UUID.randomUUID()),
                                Money.vnd(50_000),
                                Money.vnd(40_000)
                        ),
                        request
                );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(
                "REFUND_AMOUNT_MISMATCH",
                response.getBody().error().code()
        );
    }

    private HttpServletRequest request(String requestId) {
        HttpServletRequest request =
                mock(HttpServletRequest.class);

        when(
                request.getHeader("X-Request-ID")
        ).thenReturn(requestId);

        return request;
    }
}