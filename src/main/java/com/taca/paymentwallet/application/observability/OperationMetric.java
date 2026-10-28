package com.taca.paymentwallet.application.observability;

public enum OperationMetric {
    CREATE_PAYMENT("create_payment"),
    PROCESS_VNPAY_WEBHOOK("process_vnpay_webhook"),
    REQUEST_REFUND("request_refund"),
    RETRY_SETTLEMENT("retry_settlement");

    private final String tagValue;

    OperationMetric(String tagValue) {
        this.tagValue = tagValue;
    }

    public String tagValue() {
        return tagValue;
    }
}