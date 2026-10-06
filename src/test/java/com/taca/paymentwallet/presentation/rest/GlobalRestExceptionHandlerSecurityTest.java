package com.taca.paymentwallet.presentation.rest;

import com.taca.paymentwallet.application.exception.ForbiddenException;
import com.taca.paymentwallet.application.exception.MfaRequiredException;
import com.taca.paymentwallet.application.exception.UnauthenticatedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalRestExceptionHandlerSecurityTest {

    private final GlobalRestExceptionHandler handler = new GlobalRestExceptionHandler();

    @Test
    void shouldMapUnauthenticatedToUnauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-ID", "req-security-001");

        ResponseEntity<ApiErrorResponse> response = handler.handleUnauthenticated(
                new UnauthenticatedException(),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("PAYMENT_UNAUTHENTICATED");
        assertThat(response.getBody().error().message()).isEqualTo("Authentication is required");
        assertThat(response.getBody().meta().requestId()).isEqualTo("req-security-001");
    }

    @Test
    void shouldMapForbiddenToForbiddenResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-ID", "req-security-002");

        ResponseEntity<ApiErrorResponse> response = handler.handleForbidden(
                new ForbiddenException(),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("PAYMENT_FORBIDDEN");
        assertThat(response.getBody().error().message()).isEqualTo("Access is forbidden");
        assertThat(response.getBody().meta().requestId()).isEqualTo("req-security-002");
    }

    @Test
    void shouldMapMfaRequiredToUnauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-ID", "req-security-003");

        ResponseEntity<ApiErrorResponse> response = handler.handleMfaRequired(
                new MfaRequiredException(),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("AUTH_MFA_REQUIRED");
        assertThat(response.getBody().error().message()).isEqualTo("MFA step-up is required");
        assertThat(response.getBody().meta().requestId()).isEqualTo("req-security-003");
    }
}