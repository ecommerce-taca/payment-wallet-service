package com.taca.paymentwallet.presentation.rest.admin;

import com.taca.paymentwallet.application.port.in.GetAdminTaxConfigsUseCase;
import com.taca.paymentwallet.application.result.GetAdminTaxConfigResult;
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
public class AdminTaxController {

    private final GetAdminTaxConfigsUseCase getAdminTaxConfigsUseCase;
    private final FinanceOpsPolicy financeOpsPolicy;

    public AdminTaxController(
            GetAdminTaxConfigsUseCase getAdminTaxConfigsUseCase,
            FinanceOpsPolicy financeOpsPolicy
    ) {
        this.getAdminTaxConfigsUseCase = Objects.requireNonNull(getAdminTaxConfigsUseCase);
        this.financeOpsPolicy = Objects.requireNonNull(financeOpsPolicy);
    }

    @GetMapping("/taxes")
    public ResponseEntity<ApiResponse<List<AdminTaxConfigData>>> getTaxes(
            @RequestHeader("X-Request-ID") String requestId
    ) {
        financeOpsPolicy.requireFinanceOps();

        List<AdminTaxConfigData> data = getAdminTaxConfigsUseCase
                .execute()
                .stream()
                .map(this::toData)
                .toList();

        return ResponseEntity.ok(new ApiResponse<>(data, new ApiMeta(requestId)));
    }

    private AdminTaxConfigData toData(GetAdminTaxConfigResult result) {
        return new AdminTaxConfigData(
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