package com.taca.paymentwallet.infrastructure.persistence.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface SettlementCandidateProjection {

    UUID getPaymentAllocationId();

    UUID getShopId();

    UUID getWalletId();

    Long getGrossAmount();

    Long getCommissionAmount();

    Long getTaxAmount();

    Long getSellerNetAmount();

    String getCurrency();

    LocalDateTime getCreatedAt();
}