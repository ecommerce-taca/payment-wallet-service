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
@Table(name = "settlement_batch_items")
public class SettlementBatchItemJpaEntity extends UuidEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "batch_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID batchId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "shop_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID shopId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "wallet_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID walletId;

    @Column(name = "gross", nullable = false)
    private Long gross;

    @Column(name = "commission", nullable = false)
    private Long commission;

    @Column(name = "tax", nullable = false)
    private Long tax;

    @Column(name = "net", nullable = false)
    private Long net;

    @Column(name = "released_amount", nullable = false)
    private Long releasedAmount;

    @Column(name = "held_amount", nullable = false)
    private Long heldAmount;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "posting_id", columnDefinition = "BINARY(16)")
    private UUID postingId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}