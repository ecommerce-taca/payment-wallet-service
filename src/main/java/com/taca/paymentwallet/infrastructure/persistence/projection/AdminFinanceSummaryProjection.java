package com.taca.paymentwallet.infrastructure.persistence.projection;

public interface AdminFinanceSummaryProjection {

    Long getCapturedAmount();

    Long getRefundedAmount();

    Long getGrossAmount();

    Long getCommissionAmount();

    Long getTaxAmount();

    Long getSellerNetAmount();

    Long getAvailableBalance();

    Long getPendingBalance();

    Long getCompletedPayoutAmount();
}