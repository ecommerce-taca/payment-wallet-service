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
@Table(name = "wallets")
public class WalletJpaEntity extends UuidEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "shop_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID shopId;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "available_balance", nullable = false)
    private Long availableBalance;

    @Column(name = "pending_balance", nullable = false)
    private Long pendingBalance;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}