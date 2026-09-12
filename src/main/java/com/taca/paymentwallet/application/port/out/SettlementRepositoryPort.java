package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.domain.settlement.SettlementBatch;

public interface SettlementRepositoryPort {

    SettlementBatch save(SettlementBatch settlementBatch);
}