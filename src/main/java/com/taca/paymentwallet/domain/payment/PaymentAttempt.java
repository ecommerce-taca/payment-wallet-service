package com.taca.paymentwallet.domain.payment;

import com.taca.paymentwallet.domain.valueobject.PaymentAttemptId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;

import java.time.Instant;
import java.util.Locale;

public class PaymentAttempt {

    private final PaymentAttemptId id;
    private final PaymentId paymentId;
    private final String provider;
    private final String providerTransactionRef;
    private final String requestHash;
    private final String paymentUrlHash;
    private final Instant expiresAt;

    private PaymentAttemptStatus status;
    private Instant completedAt;
    private String failureCode;

    private PaymentAttempt(
            PaymentAttemptId id,
            PaymentId paymentId,
            String provider,
            String providerTransactionRef,
            PaymentAttemptStatus status,
            String requestHash,
            String paymentUrlHash,
            Instant expiresAt,
            Instant completedAt,
            String failureCode
    ) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "id must not be null"
            );
        }

        if (paymentId == null) {
            throw new IllegalArgumentException(
                    "paymentId must not be null"
            );
        }

        if (provider == null
                || provider.isBlank()) {
            throw new IllegalArgumentException(
                    "provider must not be blank"
            );
        }

        if (providerTransactionRef == null
                || providerTransactionRef.isBlank()) {
            throw new IllegalArgumentException(
                    "providerTransactionRef must not be blank"
            );
        }

        if (status == null) {
            throw new IllegalArgumentException(
                    "status must not be null"
            );
        }

        if (requestHash == null
                || requestHash.isBlank()) {
            throw new IllegalArgumentException(
                    "requestHash must not be blank"
            );
        }

        if (requestHash.length() > 64) {
            throw new IllegalArgumentException(
                    "requestHash must not exceed 64 characters"
            );
        }

        if (paymentUrlHash != null
                && paymentUrlHash.length() > 64) {
            throw new IllegalArgumentException(
                    "paymentUrlHash must not exceed 64 characters"
            );
        }

        if (expiresAt == null) {
            throw new IllegalArgumentException(
                    "expiresAt must not be null"
            );
        }

        if (status == PaymentAttemptStatus.PENDING
                && completedAt != null) {
            throw new IllegalArgumentException(
                    "pending attempt must not have completedAt"
            );
        }

        if (status != PaymentAttemptStatus.PENDING
                && completedAt == null) {
            throw new IllegalArgumentException(
                    "terminal attempt requires completedAt"
            );
        }

        if (status == PaymentAttemptStatus.FAILED
                && (failureCode == null
                || failureCode.isBlank())) {
            throw new IllegalArgumentException(
                    "failed attempt requires failureCode"
            );
        }

        if (status != PaymentAttemptStatus.FAILED
                && failureCode != null) {
            throw new IllegalArgumentException(
                    "failureCode is only allowed for failed attempt"
            );
        }

        this.id = id;
        this.paymentId = paymentId;

        this.provider =
                provider.trim()
                        .toUpperCase(Locale.ROOT);

        this.providerTransactionRef =
                providerTransactionRef.trim();

        this.status = status;
        this.requestHash = requestHash.trim();

        this.paymentUrlHash =
                paymentUrlHash == null
                        ? null
                        : paymentUrlHash.trim();

        this.expiresAt = expiresAt;
        this.completedAt = completedAt;

        this.failureCode =
                failureCode == null
                        ? null
                        : failureCode.trim();
    }

    public static PaymentAttempt create(
            PaymentAttemptId id,
            PaymentId paymentId,
            String provider,
            String providerTransactionRef,
            String requestHash,
            String paymentUrlHash,
            Instant expiresAt
    ) {
        return new PaymentAttempt(
                id,
                paymentId,
                provider,
                providerTransactionRef,
                PaymentAttemptStatus.PENDING,
                requestHash,
                paymentUrlHash,
                expiresAt,
                null,
                null
        );
    }

    public static PaymentAttempt rehydrate(
            PaymentAttemptId id,
            PaymentId paymentId,
            String provider,
            String providerTransactionRef,
            PaymentAttemptStatus status,
            String requestHash,
            String paymentUrlHash,
            Instant expiresAt,
            Instant completedAt,
            String failureCode
    ) {
        return new PaymentAttempt(
                id,
                paymentId,
                provider,
                providerTransactionRef,
                status,
                requestHash,
                paymentUrlHash,
                expiresAt,
                completedAt,
                failureCode
        );
    }

    public void markSucceeded(
            Instant completedAt
    ) {
        requirePending("mark succeeded");

        requireCompletedAt(completedAt);

        this.status = PaymentAttemptStatus.SUCCESS;

        this.completedAt = completedAt;
    }

    public void markFailed(
            String failureCode,
            Instant completedAt
    ) {
        requirePending(
                "mark failed"
        );

        if (failureCode == null
                || failureCode.isBlank()) {
            throw new IllegalArgumentException(
                    "failureCode must not be blank"
            );
        }

        requireCompletedAt(
                completedAt
        );

        this.status = PaymentAttemptStatus.FAILED;

        this.failureCode = failureCode.trim();

        this.completedAt = completedAt;
    }

    public void markExpired(
            Instant completedAt
    ) {
        requirePending(
                "mark expired"
        );

        requireCompletedAt(
                completedAt
        );

        this.status =
                PaymentAttemptStatus.EXPIRED;

        this.completedAt =
                completedAt;
    }

    private void requirePending(
            String action
    ) {
        if (status != PaymentAttemptStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot "
                            + action
                            + " payment attempt with status "
                            + status
            );
        }
    }

    private void requireCompletedAt(
            Instant completedAt
    ) {
        if (completedAt == null) {
            throw new IllegalArgumentException(
                    "completedAt must not be null"
            );
        }
    }

    public PaymentAttemptId id() {
        return id;
    }

    public PaymentId paymentId() {
        return paymentId;
    }

    public String provider() {
        return provider;
    }

    public String providerTransactionRef() {
        return providerTransactionRef;
    }

    public PaymentAttemptStatus status() {
        return status;
    }

    public String requestHash() {
        return requestHash;
    }

    public String paymentUrlHash() {
        return paymentUrlHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public String failureCode() {
        return failureCode;
    }
}