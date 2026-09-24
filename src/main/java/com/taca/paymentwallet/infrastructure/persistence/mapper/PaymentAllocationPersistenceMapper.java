package com.taca.paymentwallet.infrastructure.persistence.mapper;

import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.valueobject.FeeConfigId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.TaxConfigId;
import com.taca.paymentwallet.domain.valueobject.WalletId;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentAllocationJpaEntity;

import java.time.LocalDateTime;
import java.util.Objects;

public class PaymentAllocationPersistenceMapper {

    public PaymentAllocation toDomain(
            PaymentAllocationJpaEntity entity
    ) {
        Objects.requireNonNull(
                entity,
                "entity must not be null"
        );

        return new PaymentAllocation(
                new PaymentAllocationId(entity.getId()),
                new PaymentId(entity.getPaymentId()),
                new OrderId(entity.getOrderId()),
                new ShopId(entity.getShopId()),
                new WalletId(entity.getWalletId()),
                new Money(
                        entity.getGrossAmount(),
                        entity.getCurrency()
                ),
                new Money(
                        entity.getCommissionAmount(),
                        entity.getCurrency()
                ),
                new Money(
                        entity.getTaxAmount(),
                        entity.getCurrency()
                ),
                new Money(
                        entity.getSellerNetAmount(),
                        entity.getCurrency()
                ),
                new FeeConfigId(
                        entity.getFeeConfigId()
                ),
                new TaxConfigId(
                        entity.getTaxConfigId()
                )
        );
    }

    public PaymentAllocationJpaEntity toEntity(
            PaymentAllocation allocation,
            LocalDateTime createdAt
    ) {
        Objects.requireNonNull(
                allocation,
                "allocation must not be null"
        );

        Objects.requireNonNull(
                createdAt,
                "createdAt must not be null"
        );

        PaymentAllocationJpaEntity entity =
                new PaymentAllocationJpaEntity();

        entity.setId(
                allocation.id().value()
        );

        entity.setPaymentId(
                allocation.paymentId().value()
        );

        entity.setOrderId(
                allocation.orderId().value()
        );

        entity.setShopId(
                allocation.shopId().value()
        );

        entity.setWalletId(
                allocation.walletId().value()
        );

        entity.setGrossAmount(
                allocation.grossAmount().amount()
        );

        entity.setCommissionAmount(
                allocation.commissionAmount().amount()
        );

        entity.setTaxAmount(
                allocation.taxAmount().amount()
        );

        entity.setSellerNetAmount(
                allocation.sellerNetAmount().amount()
        );

        entity.setCurrency(
                allocation.grossAmount().currency()
        );

        entity.setFeeConfigId(
                allocation.feeConfigId().value()
        );

        entity.setTaxConfigId(
                allocation.taxConfigId().value()
        );

        entity.setCreatedAt(createdAt);

        return entity;
    }
}