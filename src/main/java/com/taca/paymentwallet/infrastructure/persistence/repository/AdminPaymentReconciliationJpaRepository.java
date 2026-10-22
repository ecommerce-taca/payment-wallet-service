package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.projection.AdminPaymentReconciliationProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AdminPaymentReconciliationJpaRepository
        extends JpaRepository<PaymentJpaEntity, UUID> {

    @Query(
            value = """
                SELECT
                    BIN_TO_UUID(p.id) AS paymentId,
                    pa.provider AS provider,
                    p.status AS localStatus,
                    pa.status AS providerStatus,
                    p.amount AS amount,
                    CASE
                        WHEN EXISTS (
                            SELECT 1
                            FROM ledger_postings lp
                            WHERE lp.reference_type = 'PAYMENT'
                              AND lp.reference_id = p.id
                              AND lp.posting_type IN ('PAYMENT_CAPTURE', 'COD_CAPTURE')
                        )
                        THEN TRUE
                        ELSE FALSE
                    END AS ledgerCaptured
                FROM payments p
                JOIN payment_attempts pa
                  ON pa.id = (
                      SELECT pa2.id
                      FROM payment_attempts pa2
                      WHERE pa2.payment_id = p.id
                      ORDER BY pa2.created_at DESC, pa2.id DESC
                      LIMIT 1
                  )
                ORDER BY p.updated_at DESC, p.id DESC
                """,
            nativeQuery = true
    )
    List<AdminPaymentReconciliationProjection> findAllForReconciliation();
}