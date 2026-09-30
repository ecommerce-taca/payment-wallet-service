package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.UnsettledSettlementCandidatePort;
import com.taca.paymentwallet.application.settlement.UnsettledSettlementCandidate;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.projection.SettlementCandidateProjection;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentAllocationJpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class SettlementCandidatePersistenceAdapter
        implements UnsettledSettlementCandidatePort {

    private final PaymentAllocationJpaRepository repository;

    public SettlementCandidatePersistenceAdapter(
            PaymentAllocationJpaRepository repository
    ) {
        this.repository =
                Objects.requireNonNull(
                        repository
                );
    }

    @Override
    public List<UnsettledSettlementCandidate> findUnsettledCandidates(
            Instant periodStart,
            Instant periodEnd
    ) {
        Objects.requireNonNull(
                periodStart,
                "periodStart must not be null"
        );

        Objects.requireNonNull(
                periodEnd,
                "periodEnd must not be null"
        );

        if (!periodStart.isBefore(periodEnd)) {
            throw new IllegalArgumentException(
                    "periodStart must be before periodEnd"
            );
        }

        return repository
                .findUnsettledCandidates(
                        PersistenceTimeMapper
                                .toLocalDateTime(
                                        periodStart
                                ),
                        PersistenceTimeMapper
                                .toLocalDateTime(
                                        periodEnd
                                )
                )
                .stream()
                .map(
                        this::toCandidate
                )
                .toList();
    }

    private UnsettledSettlementCandidate toCandidate(
            SettlementCandidateProjection projection
    ) {
        Money gross =
                new Money(
                        projection.getGrossAmount(),
                        projection.getCurrency()
                );

        Money commission =
                new Money(
                        projection.getCommissionAmount(),
                        projection.getCurrency()
                );

        Money tax =
                new Money(
                        projection.getTaxAmount(),
                        projection.getCurrency()
                );

        Money sellerNet =
                new Money(
                        projection.getSellerNetAmount(),
                        projection.getCurrency()
                );

        return new UnsettledSettlementCandidate(
                new PaymentAllocationId(
                        projection.getPaymentAllocationId()
                ),
                new ShopId(
                        projection.getShopId()
                ),
                new WalletId(
                        projection.getWalletId()
                ),
                gross,
                commission,
                tax,
                sellerNet,
                PersistenceTimeMapper.toInstant(
                        projection.getCreatedAt()
                )
        );
    }
}