package com.taca.paymentwallet.application.security;

import com.taca.paymentwallet.application.exception.MfaRequiredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MfaStepUpPolicyTest {

    private final MfaStepUpPolicy policy = new MfaStepUpPolicy();

    @AfterEach
    void tearDown() {
        MfaStepUpContextHolder.clear();
    }

    @Test
    void shouldAllowVerifiedStepUp() {
        MfaStepUpContextHolder.set(new MfaStepUpContext(true));

        assertThatCode(policy::requireVerifiedStepUp)
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectMissingStepUp() {
        assertThatThrownBy(policy::requireVerifiedStepUp)
                .isInstanceOf(MfaRequiredException.class)
                .hasMessage("MFA step-up is required");
    }

    @Test
    void shouldRejectUnverifiedStepUp() {
        MfaStepUpContextHolder.set(new MfaStepUpContext(false));

        assertThatThrownBy(policy::requireVerifiedStepUp)
                .isInstanceOf(MfaRequiredException.class);
    }
}