package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.result.CreatePaymentResult;

public interface CreatePaymentResultPayloadPort {

    String serialize(CreatePaymentResult result);

    CreatePaymentResult deserialize(String payload);
}
