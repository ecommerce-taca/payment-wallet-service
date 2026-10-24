package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.AdminPaymentReconciliationView;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.infrastructure.persistence.projection.AdminPaymentReconciliationProjection;
import com.taca.paymentwallet.infrastructure.persistence.repository.AdminPaymentReconciliationJpaRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminPaymentReconciliationReadAdapterTest {

    @Test
    void shouldMarkSuccessfulPaymentAsMatchedWhenProviderAndLedgerMatch() {
        AdminPaymentReconciliationJpaRepository repository =
                mock(AdminPaymentReconciliationJpaRepository.class);

        ClockPort clockPort = mock(ClockPort.class);

        AdminPaymentReconciliationProjection projection =
                projection(
                        "SUCCESS",
                        "SUCCESS",
                        true
                );

        Instant checkedAt =
                Instant.parse("2026-10-21T12:00:00Z");

        when(repository.findAllForReconciliation())
                .thenReturn(List.of(projection));

        when(clockPort.now()).thenReturn(checkedAt);

        AdminPaymentReconciliationReadAdapter adapter =
                new AdminPaymentReconciliationReadAdapter(
                        repository,
                        clockPort
                );

        List<AdminPaymentReconciliationView> result =
                adapter.findAll();

        assertEquals(1, result.size());
        assertTrue(result.get(0).matched());
        assertEquals(checkedAt, result.get(0).checkedAt());

        verify(repository).findAllForReconciliation();
        verify(clockPort).now();
    }

    @Test
    void shouldMarkSuccessfulPaymentAsMismatchWhenLedgerCaptureIsMissing() {
        AdminPaymentReconciliationJpaRepository repository =
                mock(AdminPaymentReconciliationJpaRepository.class);

        ClockPort clockPort = mock(ClockPort.class);

        when(repository.findAllForReconciliation())
                .thenReturn(
                        List.of(
                                projection(
                                        "SUCCESS",
                                        "SUCCESS",
                                        false
                                )
                        )
                );

        when(clockPort.now()).thenReturn(
                Instant.parse("2026-10-21T12:00:00Z")
        );

        AdminPaymentReconciliationReadAdapter adapter =
                new AdminPaymentReconciliationReadAdapter(
                        repository,
                        clockPort
                );

        assertFalse(adapter.findAll().get(0).matched());
    }

    @Test
    void shouldMarkProviderMismatch() {
        AdminPaymentReconciliationJpaRepository repository =
                mock(AdminPaymentReconciliationJpaRepository.class);

        ClockPort clockPort = mock(ClockPort.class);

        when(repository.findAllForReconciliation())
                .thenReturn(
                        List.of(
                                projection(
                                        "SUCCESS",
                                        "FAILED",
                                        true
                                )
                        )
                );

        when(clockPort.now()).thenReturn(
                Instant.parse("2026-10-21T12:00:00Z")
        );

        AdminPaymentReconciliationReadAdapter adapter =
                new AdminPaymentReconciliationReadAdapter(
                        repository,
                        clockPort
                );

        assertFalse(adapter.findAll().get(0).matched());
    }

    @Test
    void shouldAllowRefundedPaymentWithSuccessfulOriginalAttempt() {
        AdminPaymentReconciliationJpaRepository repository =
                mock(AdminPaymentReconciliationJpaRepository.class);

        ClockPort clockPort = mock(ClockPort.class);

        when(repository.findAllForReconciliation())
                .thenReturn(
                        List.of(
                                projection(
                                        "REFUNDED",
                                        "SUCCESS",
                                        true
                                )
                        )
                );

        when(clockPort.now()).thenReturn(
                Instant.parse("2026-10-21T12:00:00Z")
        );

        AdminPaymentReconciliationReadAdapter adapter =
                new AdminPaymentReconciliationReadAdapter(
                        repository,
                        clockPort
                );

        assertTrue(adapter.findAll().get(0).matched());
    }

    private AdminPaymentReconciliationProjection projection(
            String localStatus,
            String providerStatus,
            boolean ledgerCaptured
    ) {
        UUID paymentId = UUID.randomUUID();

        return new AdminPaymentReconciliationProjection() {

            @Override
            public UUID getPaymentId() {
                return paymentId;
            }

            @Override
            public String getProvider() {
                return "VNPAY";
            }

            @Override
            public String getLocalStatus() {
                return localStatus;
            }

            @Override
            public String getProviderStatus() {
                return providerStatus;
            }

            @Override
            public Long getAmount() {
                return 1_094_000L;
            }

            @Override
            public Boolean getLedgerCaptured() {
                return ledgerCaptured;
            }
        };
    }
}