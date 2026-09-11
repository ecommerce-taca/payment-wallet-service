package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.fee.PaymentFeePolicy;

public interface FeePolicyPort {

    PaymentFeePolicy currentPaymentFeePolicy();
}
