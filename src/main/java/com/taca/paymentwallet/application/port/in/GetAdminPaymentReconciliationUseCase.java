package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.query.GetAdminPaymentReconciliationQuery;
import com.taca.paymentwallet.application.result.GetAdminPaymentReconciliationResult;

public interface GetAdminPaymentReconciliationUseCase {

    GetAdminPaymentReconciliationResult execute(
            GetAdminPaymentReconciliationQuery query
    );
}