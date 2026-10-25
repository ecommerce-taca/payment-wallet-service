package com.taca.paymentwallet.support;

import com.taca.paymentwallet.application.port.out.BusinessMetricsPort;

public class FakeBusinessMetricsPort
        implements BusinessMetricsPort {

    @Override
    public void recordPayment(
            String method,
            String status,
            String action
    ) {
    }

    @Override
    public void recordRefund(
            String status,
            String action
    ) {
    }

    @Override
    public void recordPayout(
            String status,
            String action
    ) {
    }

    @Override
    public void recordSettlementRetry(String status) {
    }
}