package com.taca.paymentwallet.application.security;

import com.taca.paymentwallet.application.exception.MfaRequiredException;

public final class MfaStepUpPolicy {

    public void requireVerifiedStepUp() {
        boolean verified = MfaStepUpContextHolder.current()
                .map(MfaStepUpContext::verified)
                .orElse(false);

        if (!verified) {
            throw new MfaRequiredException();
        }
    }
}