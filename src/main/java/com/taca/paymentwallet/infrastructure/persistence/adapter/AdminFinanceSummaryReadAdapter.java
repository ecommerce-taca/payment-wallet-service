package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.AdminFinanceSummaryReadPort;
import com.taca.paymentwallet.application.port.out.AdminFinanceSummaryView;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.infrastructure.persistence.projection.AdminFinanceSummaryProjection;
import com.taca.paymentwallet.infrastructure.persistence.repository.AdminFinanceSummaryJpaRepository;

import java.util.Objects;

public class AdminFinanceSummaryReadAdapter
        implements AdminFinanceSummaryReadPort {

    private final AdminFinanceSummaryJpaRepository repository;
    private final ClockPort clockPort;

    public AdminFinanceSummaryReadAdapter(
            AdminFinanceSummaryJpaRepository repository,
            ClockPort clockPort
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    @Override
    public AdminFinanceSummaryView getSummary() {
        AdminFinanceSummaryProjection projection =
                repository.getFinanceSummary();

        return new AdminFinanceSummaryView(
                value(projection.getCapturedAmount()),
                value(projection.getRefundedAmount()),
                value(projection.getGrossAmount()),
                value(projection.getCommissionAmount()),
                value(projection.getTaxAmount()),
                value(projection.getSellerNetAmount()),
                value(projection.getAvailableBalance()),
                value(projection.getPendingBalance()),
                value(projection.getCompletedPayoutAmount()),
                clockPort.now()
        );
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}