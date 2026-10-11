package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.query.GetSellerWalletQuery;
import com.taca.paymentwallet.application.result.GetSellerWalletResult;

public interface GetSellerWalletUseCase {

    GetSellerWalletResult execute(
            GetSellerWalletQuery query
    );
}