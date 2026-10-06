package com.taca.paymentwallet.application.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MfaStepUpContextHolderTest {

    @AfterEach
    void tearDown() {
        MfaStepUpContextHolder.clear();
    }

    @Test
    void shouldExposeCurrentContext() {
        MfaStepUpContext context = new MfaStepUpContext(true);

        MfaStepUpContextHolder.set(context);

        assertThat(MfaStepUpContextHolder.current()).containsSame(context);
    }

    @Test
    void shouldClearContext() {
        MfaStepUpContextHolder.set(new MfaStepUpContext(true));

        MfaStepUpContextHolder.clear();

        assertThat(MfaStepUpContextHolder.current()).isEmpty();
    }

    @Test
    void shouldClearWhenNullIsSet() {
        MfaStepUpContextHolder.set(new MfaStepUpContext(true));

        MfaStepUpContextHolder.set(null);

        assertThat(MfaStepUpContextHolder.current()).isEmpty();
    }
}