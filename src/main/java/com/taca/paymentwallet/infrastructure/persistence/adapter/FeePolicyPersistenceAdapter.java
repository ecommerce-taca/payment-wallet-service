package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.exception.FeePolicyNotConfiguredException;
import com.taca.paymentwallet.application.fee.PaymentFeePolicy;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.FeePolicyPort;
import com.taca.paymentwallet.domain.valueobject.FeeConfigId;
import com.taca.paymentwallet.domain.valueobject.RateBps;
import com.taca.paymentwallet.domain.valueobject.TaxConfigId;
import com.taca.paymentwallet.infrastructure.persistence.entity.FeeConfigJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.TaxConfigJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.FeeConfigJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.TaxConfigJpaRepository;

import java.time.LocalDateTime;
import java.util.Objects;

public class FeePolicyPersistenceAdapter
        implements FeePolicyPort {

    private static final String PLATFORM_SCOPE =
            "PLATFORM";

    private final FeeConfigJpaRepository feeConfigRepository;
    private final TaxConfigJpaRepository taxConfigRepository;
    private final ClockPort clockPort;

    public FeePolicyPersistenceAdapter(
            FeeConfigJpaRepository feeConfigRepository,
            TaxConfigJpaRepository taxConfigRepository,
            ClockPort clockPort
    ) {
        this.feeConfigRepository =
                Objects.requireNonNull(
                        feeConfigRepository
                );

        this.taxConfigRepository =
                Objects.requireNonNull(
                        taxConfigRepository
                );

        this.clockPort =
                Objects.requireNonNull(
                        clockPort
                );
    }

    @Override
    public PaymentFeePolicy currentPaymentFeePolicy() {
        LocalDateTime effectiveAt =
                PersistenceTimeMapper
                        .toLocalDateTime(
                                clockPort.now()
                        );

        FeeConfigJpaEntity feeConfig =
                feeConfigRepository
                        .findFirstByScopeAndCategoryIdIsNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                                PLATFORM_SCOPE,
                                effectiveAt
                        )
                        .orElseThrow(
                                () ->
                                        new FeePolicyNotConfiguredException(
                                                "fee"
                                        )
                        );

        TaxConfigJpaEntity taxConfig =
                taxConfigRepository
                        .findFirstByScopeAndCategoryIdIsNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                                PLATFORM_SCOPE,
                                effectiveAt
                        )
                        .orElseThrow(
                                () ->
                                        new FeePolicyNotConfiguredException(
                                                "tax"
                                        )
                        );

        return new PaymentFeePolicy(
                new FeeConfigId(
                        feeConfig.getId()
                ),
                new TaxConfigId(
                        taxConfig.getId()
                ),
                RateBps.of(
                        feeConfig.getRateBps()
                ),
                RateBps.of(
                        taxConfig.getRateBps()
                )
        );
    }
}