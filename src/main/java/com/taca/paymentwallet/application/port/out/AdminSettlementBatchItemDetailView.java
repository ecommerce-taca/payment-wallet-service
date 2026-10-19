package com.taca.paymentwallet.application.port.out;

import java.util.List;

public record AdminSettlementBatchItemDetailView(
        AdminSettlementBatchItemView item,
        List<AdminSettlementLineView> lines
) {

    public AdminSettlementBatchItemDetailView {
        if (item == null) {
            throw new IllegalArgumentException("item must not be null");
        }

        if (lines == null) {
            throw new IllegalArgumentException("lines must not be null");
        }

        lines = List.copyOf(lines);
    }
}