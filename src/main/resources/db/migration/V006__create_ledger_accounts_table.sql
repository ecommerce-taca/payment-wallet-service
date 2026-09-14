CREATE TABLE ledger_accounts (
    id BINARY(16) NOT NULL,
    account_code VARCHAR(120) NOT NULL,
    account_type VARCHAR(60) NOT NULL,
    owner_type VARCHAR(40) NOT NULL,
    owner_id BINARY(16) NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_ledger_accounts PRIMARY KEY (id),

    CONSTRAINT uk_ledger_accounts_account_code_currency
        UNIQUE (account_code, currency),

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
        )),

    CONSTRAINT chk_ledger_accounts_owner_type
        CHECK (owner_type IN ('SYSTEM', 'SHOP')),

    CONSTRAINT chk_ledger_accounts_status
        CHECK (status IN ('ACTIVE', 'CLOSED')),

    CONSTRAINT chk_ledger_accounts_currency_vnd
        CHECK (currency = 'VND'),

    CONSTRAINT chk_ledger_accounts_owner
        CHECK (
            (owner_type = 'SYSTEM' AND owner_id IS NULL)
            OR
            (owner_type = 'SHOP' AND owner_id IS NOT NULL)
        ),

    CONSTRAINT chk_ledger_accounts_shop_account_type
        CHECK (
            (owner_type = 'SYSTEM' AND account_type IN (
                'VNPAY_CLEARING',
                'COD_CLEARING',
                'PLATFORM_COMMISSION',
                'TAX_PAYABLE',
                'PAYOUT_CLEARING',
                'REFUND_CLEARING'
            ))
            OR
            (owner_type = 'SHOP' AND account_type IN (
                'SELLER_PENDING',
                'SELLER_AVAILABLE'
            ))
        )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_ledger_accounts_owner_type_owner_id_account_type_currency
    ON ledger_accounts (owner_type, owner_id, account_type, currency);

CREATE INDEX idx_ledger_accounts_account_type_currency
    ON ledger_accounts (account_type, currency);