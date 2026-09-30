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
@Table(name = "payment_allocations")
public class PaymentAllocationJpaEntity extends UuidEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "payment_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID paymentId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "order_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID orderId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "shop_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID shopId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "wallet_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID walletId;

    @Column(name = "gross_amount", nullable = false)
    private Long grossAmount;

    @Column(name = "commission_amount", nullable = false)
    private Long commissionAmount;

    @Column(name = "tax_amount", nullable = false)
    private Long taxAmount;

    @Column(name = "seller_net_amount", nullable = false)
    private Long sellerNetAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "fee_config_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID feeConfigId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "tax_config_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID taxConfigId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}