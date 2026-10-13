package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentAllocationJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.projection.SellerRevenueAllocationProjection;
import com.taca.paymentwallet.infrastructure.persistence.projection.SettlementCandidateProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PaymentAllocationJpaRepository extends JpaRepository<PaymentAllocationJpaEntity, UUID> {

    List<PaymentAllocationJpaEntity> findByPaymentIdOrderByCreatedAtAscIdAsc(
            UUID paymentId
    );

    List<PaymentAllocationJpaEntity> findByShopId(UUID shopId);

    @Query("""
            select
                pa.id as paymentAllocationId,
                pa.shopId as shopId,
                pa.walletId as walletId,
                pa.grossAmount as grossAmount,
                pa.commissionAmount as commissionAmount,
                pa.taxAmount as taxAmount,
                pa.sellerNetAmount as sellerNetAmount,
                pa.currency as currency,
                pa.createdAt as createdAt
            from PaymentAllocationJpaEntity pa
            where pa.createdAt >= :periodStart
              and pa.createdAt < :periodEnd
              and not exists (
                    select sl.id
                    from SettlementLineJpaEntity sl
                    where sl.paymentAllocationId = pa.id
              )
            order by pa.createdAt asc, pa.id asc
            """)
    List<SettlementCandidateProjection>
    findUnsettledCandidates(
            @Param("periodStart")
            LocalDateTime periodStart,

            @Param("periodEnd")
            LocalDateTime periodEnd
    );

    @Query(
            value = """
                SELECT
                    DATE(pa.created_at) AS date,
                    SUM(pa.gross_amount) AS gross,
                    SUM(pa.commission_amount) AS commission,
                    SUM(pa.tax_amount) AS tax,
                    SUM(pa.seller_net_amount) AS net,
                    COUNT(DISTINCT pa.order_id) AS orderCount
                FROM payment_allocations pa
                WHERE pa.shop_id = :shopId
                  AND pa.created_at >= :fromAt
                  AND pa.created_at < :toAt
                GROUP BY DATE(pa.created_at)
                ORDER BY DATE(pa.created_at)
                """,
            nativeQuery = true
    )
    List<SellerRevenueAllocationProjection> findSellerRevenueByDay(
            @Param("shopId") UUID shopId,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt
    );
}