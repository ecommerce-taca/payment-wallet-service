package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.projection.AdminFinanceSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface AdminFinanceSummaryJpaRepository
        extends JpaRepository<PaymentJpaEntity, UUID> {

    @Query(
            value = """
                SELECT
                    (
                        SELECT COALESCE(SUM(p.captured_amount), 0)
                        FROM payments p
                    ) AS capturedAmount,
                    (
                        SELECT COALESCE(SUM(p.refunded_amount), 0)
                        FROM payments p
                    ) AS refundedAmount,
                    (
                        SELECT COALESCE(SUM(pa.gross_amount), 0)
                        FROM payment_allocations pa
                    ) AS grossAmount,
                    (
                        SELECT COALESCE(SUM(pa.commission_amount), 0)
                        FROM payment_allocations pa
                    ) AS commissionAmount,
                    (
                        SELECT COALESCE(SUM(pa.tax_amount), 0)
                        FROM payment_allocations pa
                    ) AS taxAmount,
                    (
                        SELECT COALESCE(SUM(pa.seller_net_amount), 0)
                        FROM payment_allocations pa
                    ) AS sellerNetAmount,
                    (
                        SELECT COALESCE(SUM(w.available_balance), 0)
                        FROM wallets w
                        WHERE w.currency = 'VND'
                    ) AS availableBalance,
                    (
                        SELECT COALESCE(SUM(w.pending_balance), 0)
                        FROM wallets w
                        WHERE w.currency = 'VND'
                    ) AS pendingBalance,
                    (
                        SELECT COALESCE(SUM(po.amount), 0)
                        FROM payouts po
                        WHERE po.status = 'SUCCESS'
                          AND po.currency = 'VND'
                    ) AS completedPayoutAmount
                """,
            nativeQuery = true
    )
    AdminFinanceSummaryProjection getFinanceSummary();
}