package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.domain.settlement.SettlementBatch;
import com.taca.paymentwallet.domain.valueobject.SettlementBatchId;

import java.util.Optional;

public interface SettlementRepositoryPort {

    SettlementBatch save(SettlementBatch settlementBatch);

    Optional<SettlementBatch> findByIdForUpdate(SettlementBatchId settlementBatchId);

    SettlementBatch update(SettlementBatch settlementBatch);
}