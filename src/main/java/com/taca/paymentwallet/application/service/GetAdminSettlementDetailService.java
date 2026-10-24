package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.exception.SettlementBatchNotFoundException;
import com.taca.paymentwallet.application.port.in.GetAdminSettlementDetailUseCase;
import com.taca.paymentwallet.application.port.out.*;
import com.taca.paymentwallet.application.query.GetAdminSettlementDetailQuery;
import com.taca.paymentwallet.application.result.*;

import java.util.Objects;

public class GetAdminSettlementDetailService
        implements GetAdminSettlementDetailUseCase {

    private final AdminSettlementReadPort readPort;

    public GetAdminSettlementDetailService(
            AdminSettlementReadPort readPort
    ) {
        this.readPort = Objects.requireNonNull(readPort);
    }

    @Override
    public GetAdminSettlementDetailResult execute(
            GetAdminSettlementDetailQuery query
    ) {
        Objects.requireNonNull(query, "query must not be null");

        AdminSettlementBatchDetailView detail =
                readPort.findDetailById(query.batchId())
                        .orElseThrow(() ->
                                new SettlementBatchNotFoundException(
                                        query.batchId()
                                )
                        );

        return toResult(detail);
    }

    private GetAdminSettlementDetailResult toResult(
            AdminSettlementBatchDetailView detail
    ) {
        AdminSettlementBatchView batch = detail.batch();

        return new GetAdminSettlementDetailResult(
                batch.id(),
                batch.periodStart(),
                batch.periodEnd(),
                batch.status(),
                batch.shopCount(),
                batch.totalGross(),
                batch.totalCommission(),
                batch.totalTax(),
                batch.totalNet(),
                batch.totalReleased(),
                batch.totalHeld(),
                batch.createdAt(),
                batch.closedAt(),
                batch.lastError(),
                detail.items()
                        .stream()
                        .map(this::toItemResult)
                        .toList()
        );
    }

    private GetAdminSettlementBatchItemResult toItemResult(
            AdminSettlementBatchItemDetailView detail
    ) {
        AdminSettlementBatchItemView item = detail.item();

        return new GetAdminSettlementBatchItemResult(
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
                detail.lines()
                        .stream()
                        .map(this::toLineResult)
                        .toList()
        );
    }

    private GetAdminSettlementLineResult toLineResult(
            AdminSettlementLineView line
    ) {
        return new GetAdminSettlementLineResult(
                line.id(),
                line.settlementBatchItemId(),
                line.paymentAllocationId(),
                line.releasedAmount(),
                line.createdAt()
        );
    }
}