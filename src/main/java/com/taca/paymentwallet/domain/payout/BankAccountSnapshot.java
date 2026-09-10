package com.taca.paymentwallet.domain.payout;

public record BankAccountSnapshot(
        String bankCode,
        String accountHolderName,
        String maskedAccountNumber
) {

    public BankAccountSnapshot {
        if (bankCode == null || bankCode.isBlank()) {
            throw new IllegalArgumentException("bankCode must not be blank");
        }

        if (accountHolderName == null || accountHolderName.isBlank()) {
            throw new IllegalArgumentException("accountHolderName must not be blank");
        }

        if (maskedAccountNumber == null || maskedAccountNumber.isBlank()) {
            throw new IllegalArgumentException("maskedAccountNumber must not be blank");
        }

        if (!maskedAccountNumber.contains("*")) {
            throw new IllegalArgumentException("bank account number must be masked");
        }
    }
}
