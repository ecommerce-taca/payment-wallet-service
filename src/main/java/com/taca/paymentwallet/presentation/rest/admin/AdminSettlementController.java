package com.taca.paymentwallet.presentation.rest.admin;

import com.taca.paymentwallet.application.port.in.GetAdminSettlementsUseCase;
import com.taca.paymentwallet.application.result.GetAdminSettlementBatchResult;
import com.taca.paymentwallet.application.security.FinanceOpsPolicy;
import com.taca.paymentwallet.presentation.rest.ApiMeta;
import com.taca.paymentwallet.presentation.rest.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminSettlementController {

    private final GetAdminSettlementsUseCase getAdminSettlementsUseCase;
    private final FinanceOpsPolicy financeOpsPolicy;

    public AdminSettlementController(
            GetAdminSettlementsUseCase getAdminSettlementsUseCase,
            FinanceOpsPolicy financeOpsPolicy
    ) {
        this.getAdminSettlementsUseCase = Objects.requireNonNull(getAdminSettlementsUseCase);
        this.financeOpsPolicy = Objects.requireNonNull(financeOpsPolicy);
    }

    @GetMapping("/settlements")
    public ResponseEntity<ApiResponse<List<AdminSettlementBatchData>>> getSettlements(
            @RequestHeader("X-Request-ID") String requestId
    ) {
        financeOpsPolicy.requireFinanceOps();

        List<AdminSettlementBatchData> data = getAdminSettlementsUseCase
                .execute()
                .stream()
                .map(this::toData)
                .toList();

        return ResponseEntity.ok(new ApiResponse<>(data, new ApiMeta(requestId)));
    }

    private AdminSettlementBatchData toData(GetAdminSettlementBatchResult result) {
        return new AdminSettlementBatchData(
                result.id(),
                result.periodStart(),
                result.periodEnd(),
                result.status(),
                result.shopCount(),
                result.totalGross(),
                result.totalCommission(),
                result.totalTax(),
                result.totalNet(),
                result.totalReleased(),
                result.totalHeld(),
                result.createdAt(),
                result.closedAt(),
                result.lastError()
        );
    }
}