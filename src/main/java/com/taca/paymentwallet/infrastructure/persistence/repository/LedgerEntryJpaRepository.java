package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.LedgerEntryJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.projection.SellerLedgerEntryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryJpaEntity, UUID> {

    List<LedgerEntryJpaEntity> findByPostingId(UUID postingId);

    @Query(
            value = """
                    WITH seller_entries AS (
                        SELECT
                            le.id,
                            le.posting_id,
                            le.entry_type,
                            le.amount,
                            le.created_at,
                            lp.posting_type,
                            lp.reference_type,
                            lp.reference_id,
                            lp.description,
                            CASE
                                WHEN le.entry_type = 'CREDIT' THEN le.amount
                                ELSE -le.amount
                            END AS signed_amount
                        FROM ledger_entries le
                        JOIN ledger_accounts la ON la.id = le.account_id
                        JOIN ledger_postings lp ON lp.id = le.posting_id
                        WHERE la.owner_type = 'SHOP'
                          AND la.owner_id = :shopId
                          AND la.currency = 'VND'
                          AND la.account_type IN ('SELLER_PENDING', 'SELLER_AVAILABLE')
                    ),
                    posting_delta AS (
                        SELECT
                            posting_id,
                            MAX(created_at) AS created_at,
                            SUM(signed_amount) AS delta
                        FROM seller_entries
                        GROUP BY posting_id
                    ),
                    posting_balance AS (
                        SELECT
                            posting_id,
                            SUM(delta) OVER (
                                ORDER BY created_at, posting_id
                                ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                            ) AS balance_after
                        FROM posting_delta
                    )
                    SELECT
                        BIN_TO_UUID(se.id) AS entryId,
                        BIN_TO_UUID(se.posting_id) AS postingId,
                        se.entry_type AS entryType,
                        se.amount AS amount,
                        pb.balance_after AS balanceAfter,
                        se.reference_type AS referenceType,
                        BIN_TO_UUID(se.reference_id) AS referenceId,
                        se.description AS description,
                        se.created_at AS createdAt
                    FROM seller_entries se
                    JOIN posting_balance pb ON pb.posting_id = se.posting_id
                    WHERE se.created_at >= :fromAt
                      AND se.created_at < :toAt
                      AND (:postingType = '' OR se.posting_type = :postingType)
                    ORDER BY se.created_at DESC, se.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM ledger_entries le
                    JOIN ledger_accounts la ON la.id = le.account_id
                    JOIN ledger_postings lp ON lp.id = le.posting_id
                    WHERE la.owner_type = 'SHOP'
                      AND la.owner_id = :shopId
                      AND la.currency = 'VND'
                      AND la.account_type IN ('SELLER_PENDING', 'SELLER_AVAILABLE')
                      AND le.created_at >= :fromAt
                      AND le.created_at < :toAt
                      AND (:postingType = '' OR lp.posting_type = :postingType)
                    """,
            nativeQuery = true
    )
    Page<SellerLedgerEntryProjection> findSellerLedger(
            @Param("shopId") UUID shopId,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt,
            @Param("postingType") String postingType,
            Pageable pageable
    );
}