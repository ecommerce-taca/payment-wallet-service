package com.taca.paymentwallet.application.port.out;

public interface BusinessMetricsPort {

    void recordPayment(
            String method,
            String status,
            String action
    );

    void recordRefund(
            String status,
            String action
    );

    void recordPayout(
            String status,
            String action
    );

    void recordSettlementRetry(String status);
}