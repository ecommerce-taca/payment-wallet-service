package com.taca.paymentwallet.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminSettlementReadPort {

    List<AdminSettlementBatchView> findAll();

    Optional<AdminSettlementBatchDetailView> findDetailById(UUID batchId);
}