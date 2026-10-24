package com.taca.paymentwallet.application.security;

import java.util.Objects;

public final class FinanceOpsPolicy {

    private final AuthorizationPolicy authorizationPolicy;
    private final MfaStepUpPolicy mfaStepUpPolicy;

    public FinanceOpsPolicy(
            AuthorizationPolicy authorizationPolicy,
            MfaStepUpPolicy mfaStepUpPolicy
    ) {
        this.authorizationPolicy = Objects.requireNonNull(authorizationPolicy);
        this.mfaStepUpPolicy = Objects.requireNonNull(mfaStepUpPolicy);
    }

    public ActorContext requireFinanceOps() {
        return authorizationPolicy.requireRole(SecurityRoles.FINANCE_OPS);
    }

    public ActorContext requireFinanceOpsWithStepUp() {
        ActorContext actor = requireFinanceOps();
        mfaStepUpPolicy.requireVerifiedStepUp();
        return actor;
    }
}