package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.exception.FeePolicyNotConfiguredException;
import com.taca.paymentwallet.application.fee.PaymentFeePolicy;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.infrastructure.persistence.entity.FeeConfigJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.TaxConfigJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.FeeConfigJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.TaxConfigJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeePolicyPersistenceAdapterTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-25T00:00:00Z"
            );

    private FeeConfigJpaRepository feeRepository;
    private TaxConfigJpaRepository taxRepository;
    private ClockPort clockPort;

    private FeePolicyPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        feeRepository =
                mock(FeeConfigJpaRepository.class);

        taxRepository =
                mock(TaxConfigJpaRepository.class);

        clockPort =
                mock(ClockPort.class);

        when(clockPort.now())
                .thenReturn(NOW);

        adapter =
                new FeePolicyPersistenceAdapter(
                        feeRepository,
                        taxRepository,
                        clockPort
                );
    }

    @Test
    void shouldReturnCurrentPlatformFeePolicy() {
        UUID feeConfigId =
                UUID.randomUUID();

        UUID taxConfigId =
                UUID.randomUUID();

        FeeConfigJpaEntity feeConfig =
                feeConfig(
                        feeConfigId,
                        700
                );

        TaxConfigJpaEntity taxConfig =
                taxConfig(
                        taxConfigId,
                        100
                );

        LocalDateTime effectiveAt =
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        0,
                        0
                );

        when(
                feeRepository
                        .findFirstByScopeAndCategoryIdIsNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                                "PLATFORM",
                                effectiveAt
                        )
        ).thenReturn(
                Optional.of(feeConfig)
        );

        when(
                taxRepository
                        .findFirstByScopeAndCategoryIdIsNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                                "PLATFORM",
                                effectiveAt
                        )
        ).thenReturn(
                Optional.of(taxConfig)
        );

        PaymentFeePolicy result =
                adapter.currentPaymentFeePolicy();

        assertEquals(
                feeConfigId,
                result.feeConfigId().value()
        );

        assertEquals(
                taxConfigId,
                result.taxConfigId().value()
        );

        assertEquals(
                700,
                result.commissionRate().value()
        );

        assertEquals(
                100,
                result.taxRate().value()
        );
    }

    @Test
    void shouldQueryPolicyUsingCurrentUtcTime() {
        LocalDateTime expected =
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        0,
                        0
                );

        when(
                feeRepository
                        .findFirstByScopeAndCategoryIdIsNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                                "PLATFORM",
                                expected
                        )
        ).thenReturn(
                Optional.of(
                        feeConfig(
                                UUID.randomUUID(),
                                700
                        )
                )
        );

        when(
                taxRepository
                        .findFirstByScopeAndCategoryIdIsNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                                "PLATFORM",
                                expected
                        )
        ).thenReturn(
                Optional.of(
                        taxConfig(
                                UUID.randomUUID(),
                                100
                        )
                )
        );

        adapter.currentPaymentFeePolicy();

        verify(clockPort, times(1))
                .now();

        verify(feeRepository)
                .findFirstByScopeAndCategoryIdIsNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        "PLATFORM",
                        expected
                );

        verify(taxRepository)
                .findFirstByScopeAndCategoryIdIsNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        "PLATFORM",
                        expected
                );
    }

    @Test
    void shouldRejectWhenFeeConfigDoesNotExist() {
        LocalDateTime effectiveAt =
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        0,
                        0
                );

        when(
                feeRepository
                        .findFirstByScopeAndCategoryIdIsNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                                "PLATFORM",
                                effectiveAt
                        )
        ).thenReturn(
                Optional.empty()
        );

        FeePolicyNotConfiguredException exception =
                assertThrows(
                        FeePolicyNotConfiguredException.class,
                        () ->
                                adapter.currentPaymentFeePolicy()
                );

        assertTrue(
                exception.getMessage()
                        .contains("fee")
        );

        verifyNoInteractions(
                taxRepository
        );
    }

    @Test
    void shouldRejectWhenTaxConfigDoesNotExist() {
        LocalDateTime effectiveAt =
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        0,
                        0
                );

        when(
                feeRepository
                        .findFirstByScopeAndCategoryIdIsNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                                "PLATFORM",
                                effectiveAt
                        )
        ).thenReturn(
                Optional.of(
                        feeConfig(
                                UUID.randomUUID(),
                                700
                        )
                )
        );

        when(
                taxRepository
                        .findFirstByScopeAndCategoryIdIsNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                                "PLATFORM",
                                effectiveAt
                        )
        ).thenReturn(
                Optional.empty()
        );

        FeePolicyNotConfiguredException exception =
                assertThrows(
                        FeePolicyNotConfiguredException.class,
                        () ->
                                adapter.currentPaymentFeePolicy()
                );

        assertTrue(
                exception.getMessage()
                        .contains("tax")
        );
    }

    @Test
    void shouldRejectInvalidRateFromPersistence() {
        LocalDateTime effectiveAt =
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        0,
                        0
                );

        when(
                feeRepository
                        .findFirstByScopeAndCategoryIdIsNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                                "PLATFORM",
                                effectiveAt
                        )
        ).thenReturn(
                Optional.of(
                        feeConfig(
                                UUID.randomUUID(),
                                10_001
                        )
                )
        );

        when(
                taxRepository
                        .findFirstByScopeAndCategoryIdIsNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                                "PLATFORM",
                                effectiveAt
                        )
        ).thenReturn(
                Optional.of(
                        taxConfig(
                                UUID.randomUUID(),
                                100
                        )
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        adapter.currentPaymentFeePolicy()
        );
    }

    private FeeConfigJpaEntity feeConfig(
            UUID id,
            int rateBps
    ) {
        FeeConfigJpaEntity entity =
                new FeeConfigJpaEntity();

        entity.setId(id);
        entity.setScope("PLATFORM");
        entity.setCategoryId(null);
        entity.setRateBps(rateBps);

        entity.setEffectiveFrom(
                LocalDateTime.of(
                        2026,
                        9,
                        1,
                        0,
                        0
                )
        );

        entity.setCreatedBy(
                UUID.randomUUID()
        );

        entity.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        1,
                        0,
                        0
                )
        );

        return entity;
    }

    private TaxConfigJpaEntity taxConfig(
            UUID id,
            int rateBps
    ) {
        TaxConfigJpaEntity entity =
                new TaxConfigJpaEntity();

        entity.setId(id);
        entity.setScope("PLATFORM");
        entity.setCategoryId(null);
        entity.setRateBps(rateBps);

        entity.setEffectiveFrom(
                LocalDateTime.of(
                        2026,
                        9,
                        1,
                        0,
                        0
                )
        );

        entity.setCreatedBy(
                UUID.randomUUID()
        );

        entity.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        1,
                        0,
                        0
                )
        );

        return entity;
    }
}