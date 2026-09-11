package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.result.RequestPayoutResult;

public interface RequestPayoutResultPayloadPort {

    String serialize(RequestPayoutResult result);

    RequestPayoutResult deserialize(String payload);
}
