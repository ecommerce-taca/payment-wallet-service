package com.taca.paymentwallet.presentation.rest;

import com.taca.paymentwallet.application.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice
public class GlobalRestExceptionHandler {

    private static final String REQUEST_ID_HEADER =
            "X-Request-ID";

    @ExceptionHandler(
            InvalidVnpaySignatureException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleInvalidVnpaySignature(
            InvalidVnpaySignatureException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "INVALID_VNPAY_SIGNATURE",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            PaymentAmountMismatchException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handlePaymentAmountMismatch(
            PaymentAmountMismatchException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.CONFLICT,
                "PAYMENT_AMOUNT_MISMATCH",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            PaymentAttemptNotFoundException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handlePaymentAttemptNotFound(
            PaymentAttemptNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.NOT_FOUND,
                "PAYMENT_ATTEMPT_NOT_FOUND",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler({
            PaymentNotFoundException.class,
            PaymentNotFoundByCheckoutGroupException.class
    })
    public ResponseEntity<ApiErrorResponse>
    handlePaymentNotFound(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.NOT_FOUND,
                "PAYMENT_NOT_FOUND",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            IdempotencyKeyReuseException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleIdempotencyKeyReuse(
            IdempotencyKeyReuseException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.CONFLICT,
                "IDEMPOTENCY_KEY_REUSED",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            RequestAlreadyProcessingException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleRequestAlreadyProcessing(
            RequestAlreadyProcessingException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.CONFLICT,
                "REQUEST_ALREADY_PROCESSING",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            UnsupportedPaymentMethodException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleUnsupportedPaymentMethod(
            UnsupportedPaymentMethodException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "UNSUPPORTED_PAYMENT_METHOD",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed",
                request
        );
    }

    @ExceptionHandler(
            MissingRequestHeaderException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleMissingRequestHeader(
            MissingRequestHeaderException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "MISSING_REQUIRED_HEADER",
                "Missing required request header: "
                        + exception.getHeaderName(),
                request
        );
    }

    @ExceptionHandler(
            HttpMessageNotReadableException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleMalformedBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST_BODY",
                "Malformed request body",
                request
        );
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthenticated(
            UnauthenticatedException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.UNAUTHORIZED,
                "PAYMENT_UNAUTHENTICATED",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(
            ForbiddenException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.FORBIDDEN,
                "PAYMENT_FORBIDDEN",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(MfaRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleMfaRequired(
            MfaRequiredException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.UNAUTHORIZED,
                "AUTH_MFA_REQUIRED",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            IllegalArgumentException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                safeMessage(
                        exception.getMessage(),
                        "Invalid request"
                ),
                request
        );
    }

    @ExceptionHandler(
            Exception.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Internal server error",
                request
        );
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        ApiErrorResponse response =
                new ApiErrorResponse(
                        new ApiError(
                                code,
                                message
                        ),
                        new ApiMeta(
                                requestId(
                                        request
                                )
                        )
                );

        return ResponseEntity
                .status(
                        status
                )
                .body(
                        response
                );
    }

    private String requestId(
            HttpServletRequest request
    ) {
        String requestId =
                request.getHeader(
                        REQUEST_ID_HEADER
                );

        if (requestId == null
                || requestId.isBlank()) {
            return UUID.randomUUID()
                    .toString();
        }

        return requestId.trim();
    }

    private String safeMessage(
            String message,
            String fallback
    ) {
        if (message == null
                || message.isBlank()) {
            return fallback;
        }

        return message;
    }
}