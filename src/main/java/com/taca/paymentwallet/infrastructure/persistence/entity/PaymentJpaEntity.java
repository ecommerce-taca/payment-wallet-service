package com.taca.paymentwallet.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "payments")
public class PaymentJpaEntity extends UuidEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "checkout_group_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID checkoutGroupId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "buyer_user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID buyerUserId;

    @Column(name = "method", nullable = false, length = 30)
    private String method;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "captured_amount", nullable = false)
    private Long capturedAmount;

    @Column(name = "refunded_amount", nullable = false)
    private Long refundedAmount;

    @Column(name = "failure_code", length = 80)
    private String failureCode;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}