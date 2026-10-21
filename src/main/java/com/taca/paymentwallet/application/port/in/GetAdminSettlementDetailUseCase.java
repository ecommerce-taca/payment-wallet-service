package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.query.GetAdminSettlementDetailQuery;
import com.taca.paymentwallet.application.result.GetAdminSettlementDetailResult;

public interface GetAdminSettlementDetailUseCase {

    GetAdminSettlementDetailResult execute(GetAdminSettlementDetailQuery query);
}