CREATE TABLE payouts (
    id BINARY(16) NOT NULL,
    wallet_id BINARY(16) NOT NULL,
    shop_id BINARY(16) NOT NULL,
    amount BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(40) NOT NULL,
    bank_account_snapshot TEXT NOT NULL,
    provider VARCHAR(40) NULL,
    provider_ref VARCHAR(120) NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    failure_code VARCHAR(80) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    requested_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_payouts PRIMARY KEY (id),

    CONSTRAINT fk_payouts_wallet_id
        FOREIGN KEY (wallet_id)
        REFERENCES wallets (id),

    CONSTRAINT uk_payouts_shop_id_idempotency_key
        UNIQUE (shop_id, idempotency_key),

    CONSTRAINT chk_payouts_amount_positive
        CHECK (amount > 0),

    CONSTRAINT chk_payouts_currency_vnd
        CHECK (currency = 'VND'),

    CONSTRAINT chk_payouts_status
        CHECK (status IN (
            'REQUESTED',
            'PROCESSING',
            'SUCCESS',
            'FAILED',
            'CANCELLED'
        )),

    CONSTRAINT chk_payouts_provider
        CHECK (provider IS NULL OR provider IN ('BANK_TRANSFER')),

    CONSTRAINT chk_payouts_version_non_negative
        CHECK (version >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_payouts_shop_id_status_requested_at
    ON payouts (shop_id, status, requested_at);

CREATE INDEX idx_payouts_wallet_id_requested_at
    ON payouts (wallet_id, requested_at);

CREATE INDEX idx_payouts_provider_provider_ref
    ON payouts (provider, provider_ref);