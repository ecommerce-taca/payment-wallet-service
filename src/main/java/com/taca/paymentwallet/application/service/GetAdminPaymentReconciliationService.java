package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.port.in.GetAdminPaymentReconciliationUseCase;
import com.taca.paymentwallet.application.port.out.AdminPaymentReconciliationReadPort;
import com.taca.paymentwallet.application.port.out.AdminPaymentReconciliationView;
import com.taca.paymentwallet.application.query.GetAdminPaymentReconciliationQuery;
import com.taca.paymentwallet.application.result.GetAdminPaymentReconciliationItemResult;
import com.taca.paymentwallet.application.result.GetAdminPaymentReconciliationResult;

import java.util.List;
import java.util.Objects;

public class GetAdminPaymentReconciliationService
        implements GetAdminPaymentReconciliationUseCase {

    private final AdminPaymentReconciliationReadPort readPort;

    public GetAdminPaymentReconciliationService(
            AdminPaymentReconciliationReadPort readPort
    ) {
        this.readPort = Objects.requireNonNull(readPort);
    }

    @Override
    public GetAdminPaymentReconciliationResult execute(
            GetAdminPaymentReconciliationQuery query
    ) {
        Objects.requireNonNull(query, "query must not be null");

        List<AdminPaymentReconciliationView> all =
                readPort.findAll();

        long mismatchCount = all.stream()
                .filter(view -> !view.matched())
                .count();

        int total = all.size();
        int totalPages = total == 0
                ? 0
                : (int) Math.ceil((double) total / query.size());

        int fromIndex = Math.min(
                (query.page() - 1) * query.size(),
                total
        );

        int toIndex = Math.min(
                fromIndex + query.size(),
                total
        );

        List<GetAdminPaymentReconciliationItemResult> items =
                all.subList(fromIndex, toIndex)
                        .stream()
                        .map(this::toResult)
                        .toList();

        return new GetAdminPaymentReconciliationResult(
                items,
                query.page(),
                query.size(),
                total,
                totalPages,
                mismatchCount
        );
    }

    private GetAdminPaymentReconciliationItemResult toResult(
            AdminPaymentReconciliationView view
    ) {
        return new GetAdminPaymentReconciliationItemResult(
                view.paymentId(),
                view.provider(),
                view.localStatus(),
                view.providerStatus(),
                view.amount(),
                view.matched(),
                view.checkedAt()
        );
    }
}