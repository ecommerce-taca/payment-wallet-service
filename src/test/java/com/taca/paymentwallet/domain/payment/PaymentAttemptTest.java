package com.taca.paymentwallet.domain.payment;

import com.taca.paymentwallet.domain.valueobject.PaymentAttemptId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentAttemptTest {

    private static final Instant EXPIRES_AT =
            Instant.parse(
                    "2026-09-27T06:30:00Z"
            );

    @Test
    void shouldCreatePendingAttempt() {
        PaymentAttempt attempt =
                createAttempt();

        assertThat(
                attempt.status()
        ).isEqualTo(
                PaymentAttemptStatus.PENDING
        );

        assertThat(
                attempt.provider()
        ).isEqualTo(
                "VNPAY"
        );

        assertThat(
                attempt.completedAt()
        ).isNull();

        assertThat(
                attempt.failureCode()
        ).isNull();
    }

    @Test
    void shouldMarkAttemptSucceeded() {
        PaymentAttempt attempt =
                createAttempt();

        Instant completedAt =
                Instant.parse(
                        "2026-09-27T06:10:00Z"
                );

        attempt.markSucceeded(
                completedAt
        );

        assertThat(
                attempt.status()
        ).isEqualTo(
                PaymentAttemptStatus.SUCCESS
        );

        assertThat(
                attempt.completedAt()
        ).isEqualTo(
                completedAt
        );

        assertThat(
                attempt.failureCode()
        ).isNull();
    }

    @Test
    void shouldMarkAttemptFailed() {
        PaymentAttempt attempt =
                createAttempt();

        Instant completedAt =
                Instant.parse(
                        "2026-09-27T06:10:00Z"
                );

        attempt.markFailed(
                "VNPAY_24_02",
                completedAt
        );

        assertThat(
                attempt.status()
        ).isEqualTo(
                PaymentAttemptStatus.FAILED
        );

        assertThat(
                attempt.failureCode()
        ).isEqualTo(
                "VNPAY_24_02"
        );

        assertThat(
                attempt.completedAt()
        ).isEqualTo(
                completedAt
        );
    }

    @Test
    void shouldRejectSecondTerminalTransition() {
        PaymentAttempt attempt =
                createAttempt();

        attempt.markSucceeded(
                Instant.parse(
                        "2026-09-27T06:10:00Z"
                )
        );

        assertThatThrownBy(
                () ->
                        attempt.markFailed(
                                "FAILED",
                                Instant.parse(
                                        "2026-09-27T06:11:00Z"
                                )
                        )
        )
                .isInstanceOf(
                        IllegalStateException.class
                );
    }

    private PaymentAttempt createAttempt() {
        return PaymentAttempt.create(
                new PaymentAttemptId(
                        UUID.randomUUID()
                ),
                new PaymentId(
                        UUID.randomUUID()
                ),
                "vnpay",
                "txn-ref-001",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
                        + "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                EXPIRES_AT
        );
    }
}