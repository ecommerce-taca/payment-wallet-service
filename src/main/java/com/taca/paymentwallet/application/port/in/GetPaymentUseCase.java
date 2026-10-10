package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.query.GetPaymentQuery;
import com.taca.paymentwallet.application.result.GetPaymentResult;

public interface GetPaymentUseCase {

    GetPaymentResult execute(GetPaymentQuery query);
}