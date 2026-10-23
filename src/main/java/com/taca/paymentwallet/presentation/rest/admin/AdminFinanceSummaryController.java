package com.taca.paymentwallet.presentation.rest.admin;

import com.taca.paymentwallet.application.port.in.GetAdminFinanceSummaryUseCase;
import com.taca.paymentwallet.application.result.GetAdminFinanceSummaryResult;
import com.taca.paymentwallet.application.security.FinanceOpsPolicy;
import com.taca.paymentwallet.presentation.rest.ApiMeta;
import com.taca.paymentwallet.presentation.rest.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/admin/finance")
public class AdminFinanceSummaryController {

    private final GetAdminFinanceSummaryUseCase useCase;
    private final FinanceOpsPolicy financeOpsPolicy;

    public AdminFinanceSummaryController(
            GetAdminFinanceSummaryUseCase useCase,
            FinanceOpsPolicy financeOpsPolicy
    ) {
        this.useCase = Objects.requireNonNull(useCase);
        this.financeOpsPolicy = Objects.requireNonNull(financeOpsPolicy);
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminFinanceSummaryData>>
    getSummary(
            @RequestHeader("X-Request-ID")
            String requestId
    ) {
        financeOpsPolicy.requireFinanceOps();

        GetAdminFinanceSummaryResult result =
                useCase.execute();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        toData(result),
                        new ApiMeta(requestId)
                )
        );
    }

    private AdminFinanceSummaryData toData(
            GetAdminFinanceSummaryResult result
    ) {
        return new AdminFinanceSummaryData(
                result.capturedAmount(),
                result.refundedAmount(),
                result.grossAmount(),
                result.commissionAmount(),
                result.taxAmount(),
                result.sellerNetAmount(),
                result.availableBalance(),
                result.pendingBalance(),
                result.completedPayoutAmount(),
                result.asOf()
        );
    }
}