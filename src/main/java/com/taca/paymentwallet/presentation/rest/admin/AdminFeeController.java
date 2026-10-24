package com.taca.paymentwallet.presentation.rest.admin;

import com.taca.paymentwallet.application.command.CreateAdminFeeConfigCommand;
import com.taca.paymentwallet.application.port.in.CreateAdminFeeConfigUseCase;
import com.taca.paymentwallet.application.port.in.GetAdminFeeConfigsUseCase;
import com.taca.paymentwallet.application.result.CreateAdminFeeConfigResult;
import com.taca.paymentwallet.application.result.GetAdminFeeConfigResult;
import com.taca.paymentwallet.application.security.ActorContext;
import com.taca.paymentwallet.application.security.FinanceOpsPolicy;
import com.taca.paymentwallet.presentation.rest.ApiMeta;
import com.taca.paymentwallet.presentation.rest.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminFeeController {

    private final GetAdminFeeConfigsUseCase getAdminFeeConfigsUseCase;
    private final CreateAdminFeeConfigUseCase createAdminFeeConfigUseCase;
    private final FinanceOpsPolicy financeOpsPolicy;

    public AdminFeeController(
            GetAdminFeeConfigsUseCase getAdminFeeConfigsUseCase,
            CreateAdminFeeConfigUseCase createAdminFeeConfigUseCase,
            FinanceOpsPolicy financeOpsPolicy
    ) {
        this.getAdminFeeConfigsUseCase =
                Objects.requireNonNull(getAdminFeeConfigsUseCase);
        this.createAdminFeeConfigUseCase =
                Objects.requireNonNull(createAdminFeeConfigUseCase);
        this.financeOpsPolicy =
                Objects.requireNonNull(financeOpsPolicy);
    }

    @GetMapping("/fees")
    public ResponseEntity<ApiResponse<List<AdminFeeConfigData>>> getFees(
            @RequestHeader("X-Request-ID") String requestId
    ) {
        financeOpsPolicy.requireFinanceOps();

        List<AdminFeeConfigData> data =
                getAdminFeeConfigsUseCase
                        .execute()
                        .stream()
                        .map(this::toData)
                        .toList();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        data,
                        new ApiMeta(requestId)
                )
        );
    }

    @PutMapping("/fees")
    public ResponseEntity<ApiResponse<AdminFeeConfigData>> updateFees(
            @RequestHeader("X-Request-ID") String requestId,
            @RequestBody AdminFeeConfigRequest request
    ) {
        ActorContext actor =
                financeOpsPolicy
                        .requireFinanceOpsWithStepUp();

        CreateAdminFeeConfigResult result =
                createAdminFeeConfigUseCase.execute(
                        new CreateAdminFeeConfigCommand(
                                request.scope(),
                                request.categoryId(),
                                request.rateBps(),
                                request.effectiveFrom(),
                                request.note(),
                                actor.userId()
                        )
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        toData(result),
                        new ApiMeta(requestId)
                )
        );
    }

    private AdminFeeConfigData toData(
            GetAdminFeeConfigResult result
    ) {
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

    private AdminFeeConfigData toData(
            CreateAdminFeeConfigResult result
    ) {
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