package com.taca.paymentwallet.infrastructure.persistence.mapper;

import com.taca.paymentwallet.domain.refund.RefundAllocation;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.RefundId;
import com.taca.paymentwallet.infrastructure.persistence.entity.RefundAllocationJpaEntity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class RefundAllocationPersistenceMapper {

    public RefundAllocation toDomain(
            RefundAllocationJpaEntity entity
    ) {
        Objects.requireNonNull(
                entity,
                "entity must not be null"
        );

        return new RefundAllocation(
                new PaymentAllocationId(
                        entity.getPaymentAllocationId()
                ),
                Money.vnd(
                        entity.getGrossAmount()
                ),
                Money.vnd(
                        entity.getCommissionReversal()
                ),
                Money.vnd(
                        entity.getTaxReversal()
                ),
                Money.vnd(
                        entity.getSellerReversal()
                )
        );
    }

    public RefundAllocationJpaEntity toEntity(
            RefundId refundId,
            RefundAllocation allocation,
            UUID rowId,
            LocalDateTime createdAt
    ) {
        Objects.requireNonNull(refundId);
        Objects.requireNonNull(allocation);
        Objects.requireNonNull(rowId);
        Objects.requireNonNull(createdAt);

        RefundAllocationJpaEntity entity =
                new RefundAllocationJpaEntity();

        entity.setId(rowId);

        entity.setRefundId(
                refundId.value()
        );

        entity.setPaymentAllocationId(
                allocation.paymentAllocationId().value()
        );

        entity.setGrossAmount(
                allocation.grossAmount().amount()
        );

        entity.setCommissionReversal(
                allocation.commissionReversal().amount()
        );

        entity.setTaxReversal(
                allocation.taxReversal().amount()
        );

        entity.setSellerReversal(
                allocation.sellerReversal().amount()
        );

        entity.setCreatedAt(createdAt);

        return entity;
    }
}