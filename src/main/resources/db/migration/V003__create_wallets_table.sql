CREATE TABLE wallets (
    id BINARY(16) NOT NULL,
    shop_id BINARY(16) NOT NULL,
    currency CHAR(3) NOT NULL,
    available_balance BIGINT NOT NULL DEFAULT 0,
    pending_balance BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(40) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_wallets PRIMARY KEY (id),

    CONSTRAINT uk_wallets_shop_id_currency
        UNIQUE (shop_id, currency),

    CONSTRAINT chk_wallets_currency_vnd
        CHECK (currency = 'VND'),

    CONSTRAINT chk_wallets_available_balance_non_negative
        CHECK (available_balance >= 0),

    CONSTRAINT chk_wallets_pending_balance_non_negative
        CHECK (pending_balance >= 0),

    CONSTRAINT chk_wallets_status
        CHECK (status IN (
            'ACTIVE',
            'SUSPENDED',
            'CLOSED'
        )),

    CONSTRAINT chk_wallets_version_non_negative
        CHECK (version >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_wallets_shop_id
    ON wallets (shop_id);

CREATE INDEX idx_wallets_status_updated_at
    ON wallets (status, updated_at);