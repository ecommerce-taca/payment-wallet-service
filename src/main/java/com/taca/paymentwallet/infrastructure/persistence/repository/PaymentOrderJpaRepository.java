package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentOrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentOrderJpaRepository extends JpaRepository<PaymentOrderJpaEntity, UUID> {

    List<PaymentOrderJpaEntity> findByPaymentId(UUID paymentId);

    Optional<PaymentOrderJpaEntity> findByOrderId(UUID orderId);

    @Query("""
        select po.paymentId
        from PaymentOrderJpaEntity po
        where po.orderId = :orderId
        """)
    Optional<UUID> findPaymentIdByOrderId(@Param("orderId") UUID orderId);
}