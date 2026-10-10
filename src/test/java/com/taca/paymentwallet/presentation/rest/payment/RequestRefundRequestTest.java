package com.taca.paymentwallet.presentation.rest.payment;

import com.taca.paymentwallet.application.command.RequestRefundCommand;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.AssertionErrors.assertEquals;

class RequestRefundRequestTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory()
                    .getValidator();

    @Test
    void shouldAcceptValidRefundRequest() {
        RequestRefundRequest request =
                new RequestRefundRequest(
                        50_000,
                        "Buyer requested refund"
                );

        assertTrue(
                validator.validate(request)
                        .isEmpty()
        );
    }

    @Test
    void shouldRejectNonPositiveAmount() {
        RequestRefundRequest request =
                new RequestRefundRequest(
                        0,
                        "Buyer requested refund"
                );

        assertFalse(
                validator.validate(request)
                        .isEmpty()
        );
    }

    @Test
    void shouldRejectBlankReason() {
        RequestRefundRequest request =
                new RequestRefundRequest(
                        50_000,
                        "   "
                );

        assertFalse(
                validator.validate(request)
                        .isEmpty()
        );
    }

    @Test
    void shouldRejectReasonLongerThan500Characters() {
        RequestRefundRequest request =
                new RequestRefundRequest(
                        50_000,
                        "a".repeat(501)
                );

        assertFalse(
                validator.validate(request)
                        .isEmpty()
        );
    }
}