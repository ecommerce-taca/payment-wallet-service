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
@Table(name = "ledger_postings")
public class LedgerPostingJpaEntity extends UuidEntity {

    @Column(name = "posting_type", nullable = false, length = 60)
    private String postingType;

    @Column(name = "business_key", nullable = false, length = 160)
    private String businessKey;

    @Column(name = "reference_type", nullable = false, length = 60)
    private String referenceType;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "reference_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID referenceId;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}