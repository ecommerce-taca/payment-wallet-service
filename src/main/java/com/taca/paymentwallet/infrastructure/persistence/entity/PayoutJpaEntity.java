package com.taca.paymentwallet.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
@Table(name = "payouts")
public class PayoutJpaEntity extends UuidEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "wallet_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID walletId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "shop_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID shopId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Lob
    @Column(name = "bank_account_snapshot", nullable = false, columnDefinition = "TEXT")
    private String bankAccountSnapshot;

    @Column(name = "provider", length = 40)
    private String provider;

    @Column(name = "provider_ref", length = 120)
    private String providerRef;

    @Column(name = "idempotency_key", nullable = false, length = 160)
    private String idempotencyKey;

    @Column(name = "failure_code", length = 80)
    private String failureCode;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}