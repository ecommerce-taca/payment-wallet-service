package com.taca.paymentwallet.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "settlement_batches")
public class SettlementBatchJpaEntity extends UuidEntity {

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "shop_count", nullable = false)
    private Integer shopCount;

    @Column(name = "total_gross", nullable = false)
    private Long totalGross;

    @Column(name = "total_commission", nullable = false)
    private Long totalCommission;

    @Column(name = "total_tax", nullable = false)
    private Long totalTax;

    @Column(name = "total_net", nullable = false)
    private Long totalNet;

    @Column(name = "total_released", nullable = false)
    private Long totalReleased;

    @Column(name = "total_held", nullable = false)
    private Long totalHeld;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;
}