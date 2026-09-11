package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.paymentevent.PaymentProviderEvent;

public interface PaymentProviderEventPort {

    boolean recordIfAbsent(PaymentProviderEvent event);

    void markApplied(String provider, String providerEventId);
}