package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.gateway.vnpay.CreateVnpayPaymentUrlRequest;
import com.taca.paymentwallet.application.gateway.vnpay.CreateVnpayPaymentUrlResult;

public interface VnpayGatewayPort {

    CreateVnpayPaymentUrlResult createPaymentUrl(CreateVnpayPaymentUrlRequest request);
}
