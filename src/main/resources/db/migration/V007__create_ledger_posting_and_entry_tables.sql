CREATE TABLE ledger_postings (
    id BINARY(16) NOT NULL,
    posting_type VARCHAR(60) NOT NULL,
    business_key VARCHAR(160) NOT NULL,
    reference_type VARCHAR(60) NOT NULL,
    reference_id BINARY(16) NOT NULL,
    description VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_ledger_postings PRIMARY KEY (id),

    CONSTRAINT uk_ledger_postings_business_key
        UNIQUE (business_key),

    CONSTRAINT chk_ledger_postings_posting_type
        CHECK (posting_type IN (
            'PAYMENT_CAPTURE',
            'COD_CAPTURE',
            'SETTLEMENT_RELEASE',
            'PAYOUT_RESERVE',
            'PAYOUT_SUCCESS',
            'PAYOUT_REVERSAL',
            'REFUND_SUCCESS'
        )),

    CONSTRAINT chk_ledger_postings_reference_type
        CHECK (reference_type IN (
            'PAYMENT',
            'REFUND',
            'PAYOUT',
            'SETTLEMENT'
        ))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_ledger_postings_reference_type_reference_id
    ON ledger_postings (reference_type, reference_id);

CREATE INDEX idx_ledger_postings_posting_type_created_at
    ON ledger_postings (posting_type, created_at);


CREATE TABLE ledger_entries (
    id BINARY(16) NOT NULL,
    posting_id BINARY(16) NOT NULL,
    account_id BINARY(16) NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_ledger_entries PRIMARY KEY (id),

    CONSTRAINT fk_ledger_entries_posting_id
        FOREIGN KEY (posting_id)
        REFERENCES ledger_postings (id),

    CONSTRAINT fk_ledger_entries_account_id
        FOREIGN KEY (account_id)
        REFERENCES ledger_accounts (id),

    CONSTRAINT chk_ledger_entries_entry_type
        CHECK (entry_type IN ('DEBIT', 'CREDIT')),

    CONSTRAINT chk_ledger_entries_amount_positive
        CHECK (amount > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_ledger_entries_posting_id
    ON ledger_entries (posting_id);

CREATE INDEX idx_ledger_entries_account_id_created_at
    ON ledger_entries (account_id, created_at);

CREATE INDEX idx_ledger_entries_entry_type_created_at
    ON ledger_entries (entry_type, created_at);