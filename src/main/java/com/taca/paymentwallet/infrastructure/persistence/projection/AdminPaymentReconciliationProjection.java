package com.taca.paymentwallet.infrastructure.persistence.projection;

import java.util.UUID;

public interface AdminPaymentReconciliationProjection {

    UUID getPaymentId();

    String getProvider();

    String getLocalStatus();

    String getProviderStatus();

    Long getAmount();

    Boolean getLedgerCaptured();
}