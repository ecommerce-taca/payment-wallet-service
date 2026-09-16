package com.taca.paymentwallet.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_events")
public class PaymentEventJpaEntity extends UuidEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "payment_id", columnDefinition = "BINARY(16)")
    private UUID paymentId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "payment_attempt_id", columnDefinition = "BINARY(16)")
    private UUID paymentAttemptId;

    @Column(name = "provider", nullable = false, length = 40)
    private String provider;

    @Column(name = "provider_event_id", nullable = false, length = 120)
    private String providerEventId;

    @Column(name = "provider_transaction_ref", length = 120)
    private String providerTransactionRef;

    @Column(name = "provider_response_code", length = 40)
    private String providerResponseCode;

    @Column(name = "provider_transaction_status", length = 40)
    private String providerTransactionStatus;

    @Column(name = "amount")
    private Long amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "failure_code", length = 80)
    private String failureCode;
}