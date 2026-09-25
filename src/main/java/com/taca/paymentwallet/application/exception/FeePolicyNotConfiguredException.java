package com.taca.paymentwallet.application.exception;

public class FeePolicyNotConfiguredException extends ApplicationException {

    public FeePolicyNotConfiguredException(String configType) {
        super(configType + " payment policy is not configured");
    }
}