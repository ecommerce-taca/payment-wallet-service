CREATE TABLE wallets (
    id BINARY(16) NOT NULL,
    shop_id BINARY(16) NOT NULL,
    currency CHAR(3) NOT NULL,
    available_balance BIGINT NOT NULL DEFAULT 0,
    pending_balance BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_wallets PRIMARY KEY (id),

    CONSTRAINT uk_wallets_shop_id_currency UNIQUE (shop_id, currency),

    CONSTRAINT chk_wallets_currency_vnd
        CHECK (currency = 'VND'),

    CONSTRAINT chk_wallets_available_balance_non_negative
        CHECK (available_balance >= 0),

    CONSTRAINT chk_wallets_pending_balance_non_negative
        CHECK (pending_balance >= 0),

    CONSTRAINT chk_wallets_status
        CHECK (status IN (
            'ACTIVE',
            'FROZEN',
            'CLOSED'
        ))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_wallets_shop_id_status
    ON wallets (shop_id, status);

CREATE TABLE ledger_accounts (
    id BINARY(16) NOT NULL,
    account_code VARCHAR(120) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    owner_type VARCHAR(30) NOT NULL,
    owner_id BINARY(16) NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_ledger_accounts PRIMARY KEY (id),

    CONSTRAINT uk_ledger_accounts_account_code_currency
        UNIQUE (account_code, currency),

    CONSTRAINT chk_ledger_accounts_currency_vnd
        CHECK (currency = 'VND'),

    CONSTRAINT chk_ledger_accounts_status
        CHECK (status IN ('ACTIVE', 'CLOSED')),

    CONSTRAINT chk_ledger_accounts_owner_type
        CHECK (owner_type IN ('SYSTEM', 'SHOP')),

    CONSTRAINT chk_ledger_accounts_owner_id
        CHECK (
            (owner_type = 'SYSTEM' AND owner_id IS NULL)
            OR
            (owner_type = 'SHOP' AND owner_id IS NOT NULL)
        ),

        CONSTRAINT chk_ledger_accounts_account_type
            CHECK (account_type IN (
                'VNPAY_CLEARING',
                'COD_CLEARING',
                'PLATFORM_COMMISSION',
                'TAX_PAYABLE',
                'SELLER_PENDING',
                'SELLER_AVAILABLE',
                'PAYOUT_CLEARING',
                'REFUND_CLEARING'
        ))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_ledger_accounts_owner
    ON ledger_accounts (owner_type, owner_id);

CREATE INDEX idx_ledger_accounts_account_type
    ON ledger_accounts (account_type);

CREATE TABLE ledger_postings (
    id BINARY(16) NOT NULL,
    posting_type VARCHAR(50) NOT NULL,
    business_key VARCHAR(200) NOT NULL,
    reference_type VARCHAR(50) NOT NULL,
    reference_id VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_ledger_postings PRIMARY KEY (id),
    CONSTRAINT uk_ledger_postings_business_key UNIQUE (business_key),

    CONSTRAINT chk_ledger_postings_posting_type
        CHECK (posting_type IN (
            'PAYMENT_CAPTURE',
            'SETTLEMENT_RELEASE',
            'PAYOUT_RESERVE',
            'PAYOUT_REVERSAL',
            'REFUND_SUCCESS'
        ))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_ledger_postings_reference
    ON ledger_postings (reference_type, reference_id);

CREATE INDEX idx_ledger_postings_created_at
    ON ledger_postings (created_at);

CREATE TABLE ledger_entries (
    id BINARY(16) NOT NULL,
    posting_id BINARY(16) NOT NULL,
    account_id BINARY(16) NOT NULL,
    entry_type VARCHAR(10) NOT NULL,
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