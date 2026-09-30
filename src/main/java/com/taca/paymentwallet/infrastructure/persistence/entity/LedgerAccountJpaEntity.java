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
@Table(name = "ledger_accounts")
public class LedgerAccountJpaEntity extends UuidEntity {

    @Column(name = "account_code", nullable = false, length = 120)
    private String accountCode;

    @Column(name = "account_type", nullable = false, length = 60)
    private String accountType;

    @Column(name = "owner_type", nullable = false, length = 40)
    private String ownerType;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "owner_id", columnDefinition = "BINARY(16)")
    private UUID ownerId;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}