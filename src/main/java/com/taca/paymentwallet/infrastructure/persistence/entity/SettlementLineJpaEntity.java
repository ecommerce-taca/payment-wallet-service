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
@Table(name = "settlement_lines")
public class SettlementLineJpaEntity extends UuidEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "settlement_batch_item_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID settlementBatchItemId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "payment_allocation_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID paymentAllocationId;

    @Column(name = "released_amount", nullable = false)
    private Long releasedAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}