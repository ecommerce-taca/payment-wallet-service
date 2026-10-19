package com.taca.paymentwallet.presentation.rest.admin;

import com.taca.paymentwallet.application.port.in.GetAdminFeeConfigsUseCase;
import com.taca.paymentwallet.application.result.GetAdminFeeConfigResult;
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
public class AdminFeeController {

    private final GetAdminFeeConfigsUseCase getAdminFeeConfigsUseCase;
    private final FinanceOpsPolicy financeOpsPolicy;

    public AdminFeeController(
            GetAdminFeeConfigsUseCase getAdminFeeConfigsUseCase,
            FinanceOpsPolicy financeOpsPolicy
    ) {
        this.getAdminFeeConfigsUseCase = Objects.requireNonNull(getAdminFeeConfigsUseCase);
        this.financeOpsPolicy = Objects.requireNonNull(financeOpsPolicy);
    }

    @GetMapping("/fees")
    public ResponseEntity<ApiResponse<List<AdminFeeConfigData>>> getFees(
            @RequestHeader("X-Request-ID") String requestId
    ) {
        financeOpsPolicy.requireFinanceOps();

        List<AdminFeeConfigData> data = getAdminFeeConfigsUseCase
                .execute()
                .stream()
                .map(this::toData)
                .toList();

        return ResponseEntity.ok(new ApiResponse<>(data, new ApiMeta(requestId)));
    }

    private AdminFeeConfigData toData(GetAdminFeeConfigResult result) {
        return new AdminFeeConfigData(
                result.id(),
                result.scope(),
                result.categoryId(),
                result.rateBps(),
                result.effectiveFrom(),
                result.note(),
                result.createdBy(),
                result.createdAt()
        );
    }
}