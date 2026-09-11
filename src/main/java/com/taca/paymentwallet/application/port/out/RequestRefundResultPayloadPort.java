package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.result.RequestRefundResult;

public interface RequestRefundResultPayloadPort {

    String serialize(RequestRefundResult result);

    RequestRefundResult deserialize(String payload);
}
