package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.result.GetAdminFeeConfigResult;

import java.util.List;

public interface GetAdminFeeConfigsUseCase {

    List<GetAdminFeeConfigResult> execute();
}