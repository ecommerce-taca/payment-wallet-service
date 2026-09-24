package com.taca.paymentwallet.infrastructure.persistence.mapper;

import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.payment.PaymentMethod;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.payment.PaymentStatus;
import com.taca.paymentwallet.domain.valueobject.BuyerUserId;
import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentOrderJpaEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PaymentPersistenceMapper {

    public Payment toDomain(
            PaymentJpaEntity paymentEntity,
            List<PaymentOrderJpaEntity> orderEntities
    ) {
        Objects.requireNonNull(
                paymentEntity,
                "paymentEntity must not be null"
        );

        Objects.requireNonNull(
                orderEntities,
                "orderEntities must not be null"
        );

        List<PaymentOrder> orders = orderEntities.stream()
                .map(this::toDomainOrder)
                .toList();

        return Payment.rehydrate(
                new PaymentId(paymentEntity.getId()),
                new CheckoutGroupId(
                        paymentEntity.getCheckoutGroupId()
                ),
                new BuyerUserId(
                        paymentEntity.getBuyerUserId()
                ),
                PaymentMethod.valueOf(
                        paymentEntity.getMethod()
                ),
                new Money(
                        paymentEntity.getAmount(),
                        paymentEntity.getCurrency()
                ),
                orders,
                PaymentStatus.valueOf(
                        paymentEntity.getStatus()
                ),
                new Money(
                        paymentEntity.getCapturedAmount(),
                        paymentEntity.getCurrency()
                ),
                new Money(
                        paymentEntity.getRefundedAmount(),
                        paymentEntity.getCurrency()
                ),
                paymentEntity.getFailureCode(),
                PersistenceTimeMapper.toInstant(
                        paymentEntity.getExpiresAt()
                ),
                PersistenceTimeMapper.toInstant(
                        paymentEntity.getPaidAt()
                )
        );
    }

    public PaymentJpaEntity toNewEntity(
            Payment payment,
            LocalDateTime createdAt
    ) {
        Objects.requireNonNull(
                payment,
                "payment must not be null"
        );

        Objects.requireNonNull(
                createdAt,
                "createdAt must not be null"
        );

        PaymentJpaEntity entity =
                new PaymentJpaEntity();

        entity.setId(payment.id().value());
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);

        copyDomainState(
                payment,
                entity
        );

        return entity;
    }

    public void updateEntity(
            Payment payment,
            PaymentJpaEntity entity,
            LocalDateTime updatedAt
    ) {
        Objects.requireNonNull(
                payment,
                "payment must not be null"
        );

        Objects.requireNonNull(
                entity,
                "entity must not be null"
        );

        Objects.requireNonNull(
                updatedAt,
                "updatedAt must not be null"
        );

        if (!payment.id().value().equals(entity.getId())) {
            throw new IllegalArgumentException(
                    "payment id does not match entity id"
            );
        }

        copyDomainState(
                payment,
                entity
        );

        entity.setUpdatedAt(updatedAt);
    }

    public PaymentOrder toDomainOrder(
            PaymentOrderJpaEntity entity
    ) {
        Objects.requireNonNull(
                entity,
                "entity must not be null"
        );

        return new PaymentOrder(
                new OrderId(entity.getOrderId()),
                new ShopId(entity.getShopId()),
                new Money(
                        entity.getAmount(),
                        entity.getCurrency()
                )
        );
    }

    public PaymentOrderJpaEntity toOrderEntity(
            PaymentId paymentId,
            PaymentOrder order,
            UUID rowId,
            LocalDateTime createdAt
    ) {
        Objects.requireNonNull(
                paymentId,
                "paymentId must not be null"
        );

        Objects.requireNonNull(
                order,
                "order must not be null"
        );

        Objects.requireNonNull(
                rowId,
                "rowId must not be null"
        );

        Objects.requireNonNull(
                createdAt,
                "createdAt must not be null"
        );

        PaymentOrderJpaEntity entity =
                new PaymentOrderJpaEntity();

        entity.setId(rowId);
        entity.setPaymentId(paymentId.value());
        entity.setOrderId(order.orderId().value());
        entity.setShopId(order.shopId().value());
        entity.setAmount(order.amount().amount());
        entity.setCurrency(order.amount().currency());
        entity.setCreatedAt(createdAt);

        return entity;
    }

    private void copyDomainState(
            Payment payment,
            PaymentJpaEntity entity
    ) {
        entity.setCheckoutGroupId(
                payment.checkoutGroupId().value()
        );

        entity.setBuyerUserId(
                payment.buyerUserId().value()
        );

        entity.setMethod(
                payment.method().name()
        );

        entity.setAmount(
                payment.amount().amount()
        );

        entity.setCurrency(
                payment.amount().currency()
        );

        entity.setStatus(
                payment.status().name()
        );

        entity.setCapturedAmount(
                payment.capturedAmount().amount()
        );

        entity.setRefundedAmount(
                payment.refundedAmount().amount()
        );

        entity.setFailureCode(
                payment.failureCode()
        );

        entity.setExpiresAt(
                PersistenceTimeMapper.toLocalDateTime(
                        payment.expiresAt()
                )
        );

        entity.setPaidAt(
                PersistenceTimeMapper.toLocalDateTime(
                        payment.paidAt()
                )
        );
    }
}