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
@Table(name = "ledger_entries")
public class LedgerEntryJpaEntity extends UuidEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "posting_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID postingId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "account_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID accountId;

    @Column(name = "entry_type", nullable = false, length = 20)
    private String entryType;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}