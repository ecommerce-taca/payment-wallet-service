package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentEventJpaRepository extends JpaRepository<PaymentEventJpaEntity, UUID> {

    Optional<PaymentEventJpaEntity> findByProviderAndProviderEventId(
            String provider,
            String providerEventId
    );

    List<PaymentEventJpaEntity> findByPaymentIdOrderByReceivedAtDesc(UUID paymentId);

    @Modifying
    @Query(value = """
                INSERT IGNORE INTO payment_events (
                    id,
                    payment_id,
                    payment_attempt_id,
                    provider,
                    provider_event_id,
                    provider_transaction_ref,
                    provider_response_code,
                    provider_transaction_status,
                    amount,
                    currency,
                    payload_hash,
                    received_at,
                    applied_at,
                    status,
                    failure_code
                )
                VALUES (
                    UNHEX(REPLACE(:id, '-', '')),
                    UNHEX(REPLACE(:paymentId, '-', '')),
                    NULL,
                    :provider,
                    :providerEventId,
                    :providerTransactionRef,
                    :providerResponseCode,
                    :providerTransactionStatus,
                    :amount,
                    :currency,
                    :payloadHash,
                    :receivedAt,
                    NULL,
                    :status,
                    NULL
                )
                """,
            nativeQuery = true
    )
    int insertIgnoreProviderEvent(
            @Param("id") String id,
            @Param("paymentId") String paymentId,
            @Param("provider") String provider,
            @Param("providerEventId") String providerEventId,
            @Param("providerTransactionRef")
            String providerTransactionRef,
            @Param("providerResponseCode")
            String providerResponseCode,
            @Param("providerTransactionStatus")
            String providerTransactionStatus,
            @Param("amount") long amount,
            @Param("currency") String currency,
            @Param("payloadHash") String payloadHash,
            @Param("receivedAt") LocalDateTime receivedAt,
            @Param("status") String status
    );
}