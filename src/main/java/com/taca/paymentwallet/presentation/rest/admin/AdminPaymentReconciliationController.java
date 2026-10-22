package com.taca.paymentwallet.presentation.rest.admin;

import com.taca.paymentwallet.application.port.in.GetAdminPaymentReconciliationUseCase;
import com.taca.paymentwallet.application.query.GetAdminPaymentReconciliationQuery;
import com.taca.paymentwallet.application.result.GetAdminPaymentReconciliationItemResult;
import com.taca.paymentwallet.application.result.GetAdminPaymentReconciliationResult;
import com.taca.paymentwallet.application.security.FinanceOpsPolicy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/admin/payments")
public class AdminPaymentReconciliationController {

    private final GetAdminPaymentReconciliationUseCase useCase;
    private final FinanceOpsPolicy financeOpsPolicy;

    public AdminPaymentReconciliationController(
            GetAdminPaymentReconciliationUseCase useCase,
            FinanceOpsPolicy financeOpsPolicy
    ) {
        this.useCase = Objects.requireNonNull(useCase);
        this.financeOpsPolicy = Objects.requireNonNull(financeOpsPolicy);
    }

    @GetMapping("/reconciliation")
    public ResponseEntity<AdminPaymentReconciliationResponse> getReconciliation(
            @RequestHeader("X-Request-ID") String requestId,
            @RequestParam(defaultValue = "1") String page,
            @RequestParam(defaultValue = "20") String size
    ) {
        financeOpsPolicy.requireFinanceOps();

        GetAdminPaymentReconciliationResult result =
                useCase.execute(
                        new GetAdminPaymentReconciliationQuery(
                                parseInteger(page, "page"),
                                parseInteger(size, "size")
                        )
                );

        return ResponseEntity.ok(
                new AdminPaymentReconciliationResponse(
                        result.items().stream()
                                .map(this::toData)
                                .toList(),
                        new AdminPaymentReconciliationMeta(
                                requestId,
                                result.page(),
                                result.size(),
                                result.total(),
                                result.totalPages(),
                                result.mismatchCount()
                        )
                )
        );
    }

    private AdminPaymentReconciliationData toData(
            GetAdminPaymentReconciliationItemResult result
    ) {
        return new AdminPaymentReconciliationData(
                result.paymentId(),
                result.provider(),
                result.localStatus(),
                result.providerStatus()
                        .toLowerCase(Locale.ROOT),
                result.amount(),
                result.matched(),
                result.checkedAt()
        );
    }

    private int parseInteger(
            String value,
            String name
    ) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    name + " must be a valid integer"
            );
        }
    }
}