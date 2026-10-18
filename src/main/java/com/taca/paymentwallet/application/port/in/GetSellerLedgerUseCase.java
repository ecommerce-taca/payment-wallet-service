package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.query.GetSellerLedgerQuery;
import com.taca.paymentwallet.application.result.GetSellerLedgerResult;

public interface GetSellerLedgerUseCase {

    GetSellerLedgerResult execute(GetSellerLedgerQuery query);
}