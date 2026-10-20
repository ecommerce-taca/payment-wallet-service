package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.result.GetAdminSettlementBatchResult;

import java.util.List;

public interface GetAdminSettlementsUseCase {

    List<GetAdminSettlementBatchResult> execute();
}