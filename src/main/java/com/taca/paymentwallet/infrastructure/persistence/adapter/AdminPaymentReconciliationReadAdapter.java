package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.AdminPaymentReconciliationReadPort;
import com.taca.paymentwallet.application.port.out.AdminPaymentReconciliationView;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.infrastructure.persistence.projection.AdminPaymentReconciliationProjection;
import com.taca.paymentwallet.infrastructure.persistence.repository.AdminPaymentReconciliationJpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class AdminPaymentReconciliationReadAdapter
        implements AdminPaymentReconciliationReadPort {

    private final AdminPaymentReconciliationJpaRepository repository;
    private final ClockPort clockPort;

    public AdminPaymentReconciliationReadAdapter(
            AdminPaymentReconciliationJpaRepository repository,
            ClockPort clockPort
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    @Override
    public List<AdminPaymentReconciliationView> findAll() {
        Instant checkedAt = clockPort.now();

        return repository
                .findAllForReconciliation()
                .stream()
                .map(projection -> toView(projection, checkedAt))
                .toList();
    }

    private AdminPaymentReconciliationView toView(
            AdminPaymentReconciliationProjection projection,
            Instant checkedAt
    ) {
        return new AdminPaymentReconciliationView(
                projection.getPaymentId(),
                projection.getProvider(),
                projection.getLocalStatus(),
                projection.getProviderStatus(),
                projection.getAmount(),
                matches(projection),
                checkedAt
        );
    }

    private boolean matches(
            AdminPaymentReconciliationProjection projection
    ) {
        String localStatus = projection.getLocalStatus();
        String providerStatus = projection.getProviderStatus();
        boolean ledgerCaptured =
                Boolean.TRUE.equals(projection.getLedgerCaptured());

        if ("SUCCESS".equals(localStatus)) {
            return "SUCCESS".equals(providerStatus)
                    && ledgerCaptured;
        }

        if ("FAILED".equals(localStatus)) {
            return "FAILED".equals(providerStatus)
                    && !ledgerCaptured;
        }

        if ("EXPIRED".equals(localStatus)) {
            return "EXPIRED".equals(providerStatus)
                    && !ledgerCaptured;
        }

        if ("PENDING".equals(localStatus)) {
            return "PENDING".equals(providerStatus)
                    && !ledgerCaptured;
        }

        if ("PARTIALLY_REFUNDED".equals(localStatus)
                || "REFUNDED".equals(localStatus)) {
            return "SUCCESS".equals(providerStatus)
                    && ledgerCaptured;
        }

        return false;
    }
}