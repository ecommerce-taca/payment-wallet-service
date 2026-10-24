package com.taca.paymentwallet.application.observability;

public enum OperationResult {
    SUCCESS("success"),
    FAILURE("failure"),
    DUPLICATE("duplicate"),
    REJECTED("rejected");

    private final String tagValue;

    OperationResult(String tagValue) {
        this.tagValue = tagValue;
    }

    public String tagValue() {
        return tagValue;
    }
}