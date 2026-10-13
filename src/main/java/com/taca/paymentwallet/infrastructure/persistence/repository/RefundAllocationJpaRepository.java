package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.RefundAllocationJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.projection.SellerRevenueRefundProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface RefundAllocationJpaRepository extends JpaRepository<RefundAllocationJpaEntity, UUID> {

    List<RefundAllocationJpaEntity> findByRefundId(UUID refundId);

    @Query(
            value = """
                    SELECT
                        DATE(ra.created_at) AS date,
                        SUM(ra.gross_amount) AS refunded
                    FROM refund_allocations ra
                    JOIN payment_allocations pa ON pa.id = ra.payment_allocation_id
                    WHERE pa.shop_id = :shopId
                      AND ra.created_at >= :fromAt
                      AND ra.created_at < :toAt
                    GROUP BY DATE(ra.created_at)
                    ORDER BY DATE(ra.created_at)
                    """,
            nativeQuery = true
    )
    List<SellerRevenueRefundProjection> findSellerRefundedRevenueByDay(
            @Param("shopId") UUID shopId,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt
    );
}