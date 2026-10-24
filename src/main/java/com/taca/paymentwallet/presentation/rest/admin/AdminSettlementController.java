package com.taca.paymentwallet.presentation.rest.admin;

import com.taca.paymentwallet.application.command.RetryAdminSettlementCommand;
import com.taca.paymentwallet.application.port.in.GetAdminSettlementDetailUseCase;
import com.taca.paymentwallet.application.port.in.GetAdminSettlementsUseCase;
import com.taca.paymentwallet.application.port.in.RetryAdminSettlementUseCase;
import com.taca.paymentwallet.application.query.GetAdminSettlementDetailQuery;
import com.taca.paymentwallet.application.result.*;
import com.taca.paymentwallet.application.security.ActorContext;
import com.taca.paymentwallet.application.security.FinanceOpsPolicy;
import com.taca.paymentwallet.presentation.rest.ApiMeta;
import com.taca.paymentwallet.presentation.rest.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminSettlementController {

    private final GetAdminSettlementsUseCase getAdminSettlementsUseCase;
    private final FinanceOpsPolicy financeOpsPolicy;
    private final GetAdminSettlementDetailUseCase getAdminSettlementDetailUseCase;
    private final RetryAdminSettlementUseCase retryAdminSettlementUseCase;

    public AdminSettlementController(
            GetAdminSettlementsUseCase getAdminSettlementsUseCase,
            GetAdminSettlementDetailUseCase getAdminSettlementDetailUseCase,
            RetryAdminSettlementUseCase retryAdminSettlementUseCase,
            FinanceOpsPolicy financeOpsPolicy
    ) {
        this.getAdminSettlementsUseCase = Objects.requireNonNull(getAdminSettlementsUseCase);
        this.getAdminSettlementDetailUseCase = Objects.requireNonNull(getAdminSettlementDetailUseCase);
        this.retryAdminSettlementUseCase = Objects.requireNonNull(retryAdminSettlementUseCase);
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

    @GetMapping("/settlements/{batchId}")
    public ResponseEntity<ApiResponse<AdminSettlementDetailData>>
    getSettlementDetail(
            @PathVariable String batchId,
            @RequestHeader("X-Request-ID") String requestId
    ) {
        financeOpsPolicy.requireFinanceOps();

        GetAdminSettlementDetailResult result =
                getAdminSettlementDetailUseCase.execute(
                        new GetAdminSettlementDetailQuery(
                                parseBatchId(batchId)
                        )
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        toDetailData(result),
                        new ApiMeta(requestId)
                )
        );
    }

    @PostMapping("/settlements/{batchId}/retry")
    public ResponseEntity<ApiResponse<RetryAdminSettlementData>> retrySettlement(
            @PathVariable String batchId,
            @RequestHeader("X-Request-ID") String requestId
    ) {
        ActorContext actor = financeOpsPolicy.requireFinanceOpsWithStepUp();

        RetryAdminSettlementResult result = retryAdminSettlementUseCase.execute(
                new RetryAdminSettlementCommand(parseBatchId(batchId), actor.userId())
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        new RetryAdminSettlementData(
                                result.batchId(),
                                result.status(),
                                result.retriedItemCount()
                        ),
                        new ApiMeta(requestId)
                )
        );
    }

    private AdminSettlementDetailData toDetailData(
            GetAdminSettlementDetailResult result
    ) {
        return new AdminSettlementDetailData(
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
                result.lastError(),
                result.items()
                        .stream()
                        .map(this::toItemData)
                        .toList()
        );
    }

    private AdminSettlementBatchItemData toItemData(
            GetAdminSettlementBatchItemResult item
    ) {
        return new AdminSettlementBatchItemData(
                item.id(),
                item.batchId(),
                item.shopId(),
                item.walletId(),
                item.gross(),
                item.commission(),
                item.tax(),
                item.net(),
                item.releasedAmount(),
                item.heldAmount(),
                item.status(),
                item.postingId(),
                item.createdAt(),
                item.lines()
                        .stream()
                        .map(this::toLineData)
                        .toList()
        );
    }

    private AdminSettlementLineData toLineData(
            GetAdminSettlementLineResult line
    ) {
        return new AdminSettlementLineData(
                line.id(),
                line.settlementBatchItemId(),
                line.paymentAllocationId(),
                line.releasedAmount(),
                line.createdAt()
        );
    }

    private UUID parseBatchId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "batchId must be a valid UUID"
            );
        }
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