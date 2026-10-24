package com.taca.paymentwallet.application.port.out;

import java.util.List;

public record AdminSettlementBatchDetailView(
        AdminSettlementBatchView batch,
        List<AdminSettlementBatchItemDetailView> items
) {

    public AdminSettlementBatchDetailView {
        if (batch == null) {
            throw new IllegalArgumentException("batch must not be null");
        }

        if (items == null) {
            throw new IllegalArgumentException("items must not be null");
        }

        items = List.copyOf(items);
    }
}