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
@Table(name = "refund_allocations")
public class RefundAllocationJpaEntity extends UuidEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "refund_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID refundId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "payment_allocation_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID paymentAllocationId;

    @Column(name = "gross_amount", nullable = false)
    private Long grossAmount;

    @Column(name = "commission_reversal", nullable = false)
    private Long commissionReversal;

    @Column(name = "tax_reversal", nullable = false)
    private Long taxReversal;

    @Column(name = "seller_reversal", nullable = false)
    private Long sellerReversal;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}