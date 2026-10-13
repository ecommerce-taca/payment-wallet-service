package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.query.GetSellerRevenueQuery;
import com.taca.paymentwallet.application.result.GetSellerRevenueResult;

public interface GetSellerRevenueUseCase {

    GetSellerRevenueResult execute(GetSellerRevenueQuery query);
}