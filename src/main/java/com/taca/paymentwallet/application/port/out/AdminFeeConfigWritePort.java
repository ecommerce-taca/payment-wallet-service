package com.taca.paymentwallet.application.port.out;

public interface AdminFeeConfigWritePort {

    AdminFeeConfigView append(AdminFeeConfigWriteModel model);
}