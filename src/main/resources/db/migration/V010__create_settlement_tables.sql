CREATE TABLE settlement_batches (
    id BINARY(16) NOT NULL,
    period_start DATETIME(6) NOT NULL,
    period_end DATETIME(6) NOT NULL,
    status VARCHAR(40) NOT NULL,
    shop_count INT NOT NULL DEFAULT 0,
    total_gross BIGINT NOT NULL DEFAULT 0,
    total_commission BIGINT NOT NULL DEFAULT 0,
    total_tax BIGINT NOT NULL DEFAULT 0,
    total_net BIGINT NOT NULL DEFAULT 0,
    total_released BIGINT NOT NULL DEFAULT 0,
    total_held BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    closed_at DATETIME(6) NULL,
    last_error VARCHAR(1000) NULL,

    CONSTRAINT pk_settlement_batches PRIMARY KEY (id),

    CONSTRAINT uk_settlement_batches_period_start_period_end
        UNIQUE (period_start, period_end),

    CONSTRAINT chk_settlement_batches_period
        CHECK (period_start < period_end),

    CONSTRAINT chk_settlement_batches_status
        CHECK (status IN (
            'PENDING',
            'PROCESSING',
            'COMPLETED',
            'FAILED'
        )),

    CONSTRAINT chk_settlement_batches_shop_count_non_negative
        CHECK (shop_count >= 0),

    CONSTRAINT chk_settlement_batches_total_gross_non_negative
        CHECK (total_gross >= 0),

    CONSTRAINT chk_settlement_batches_total_commission_non_negative
        CHECK (total_commission >= 0),

    CONSTRAINT chk_settlement_batches_total_tax_non_negative
        CHECK (total_tax >= 0),

    CONSTRAINT chk_settlement_batches_total_net_non_negative
        CHECK (total_net >= 0),

    CONSTRAINT chk_settlement_batches_total_released_non_negative
        CHECK (total_released >= 0),

    CONSTRAINT chk_settlement_batches_total_held_non_negative
        CHECK (total_held >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_settlement_batches_status_period_start
    ON settlement_batches (status, period_start);


CREATE TABLE settlement_batch_items (
    id BINARY(16) NOT NULL,
    batch_id BINARY(16) NOT NULL,
    shop_id BINARY(16) NOT NULL,
    wallet_id BINARY(16) NOT NULL,
    gross BIGINT NOT NULL DEFAULT 0,
    commission BIGINT NOT NULL DEFAULT 0,
    tax BIGINT NOT NULL DEFAULT 0,
    net BIGINT NOT NULL DEFAULT 0,
    released_amount BIGINT NOT NULL DEFAULT 0,
    held_amount BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(40) NOT NULL,
    posting_id BINARY(16) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_settlement_batch_items PRIMARY KEY (id),

    CONSTRAINT fk_settlement_batch_items_batch_id
        FOREIGN KEY (batch_id)
            REFERENCES settlement_batches (id),

    CONSTRAINT fk_settlement_batch_items_wallet_id
        FOREIGN KEY (wallet_id)
            REFERENCES wallets (id),

    CONSTRAINT fk_settlement_batch_items_posting_id
        FOREIGN KEY (posting_id)
            REFERENCES ledger_postings (id),

    CONSTRAINT uk_settlement_batch_items_batch_id_shop_id
        UNIQUE (batch_id, shop_id),

    CONSTRAINT chk_settlement_batch_items_status
        CHECK (status IN (
            'PENDING',
            'COMPLETED',
            'FAILED'
        )),

    CONSTRAINT chk_settlement_batch_items_gross_non_negative
        CHECK (gross >= 0),

    CONSTRAINT chk_settlement_batch_items_commission_non_negative
        CHECK (commission >= 0),

    CONSTRAINT chk_settlement_batch_items_tax_non_negative
        CHECK (tax >= 0),

    CONSTRAINT chk_settlement_batch_items_net_non_negative
        CHECK (net >= 0),

    CONSTRAINT chk_settlement_batch_items_released_amount_non_negative
        CHECK (released_amount >= 0),

    CONSTRAINT chk_settlement_batch_items_held_amount_non_negative
        CHECK (held_amount >= 0),

    CONSTRAINT chk_settlement_batch_items_amount_balanced
        CHECK (gross = commission + tax + net),

    CONSTRAINT chk_settlement_batch_items_net_balanced
        CHECK (net = released_amount + held_amount)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_settlement_batch_items_batch_id
    ON settlement_batch_items (batch_id);

CREATE INDEX idx_settlement_batch_items_shop_id_created_at
    ON settlement_batch_items (shop_id, created_at);

CREATE INDEX idx_settlement_batch_items_wallet_id_created_at
    ON settlement_batch_items (wallet_id, created_at);


CREATE TABLE settlement_lines (
    id BINARY(16) NOT NULL,
    settlement_batch_item_id BINARY(16) NOT NULL,
    payment_allocation_id BINARY(16) NOT NULL,
    released_amount BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_settlement_lines PRIMARY KEY (id),

    CONSTRAINT fk_settlement_lines_settlement_batch_item_id
        FOREIGN KEY (settlement_batch_item_id)
        REFERENCES settlement_batch_items (id),

    CONSTRAINT fk_settlement_lines_payment_allocation_id
        FOREIGN KEY (payment_allocation_id)
        REFERENCES payment_allocations (id),

    CONSTRAINT uk_settlement_lines_payment_allocation_id
        UNIQUE (payment_allocation_id),

    CONSTRAINT chk_settlement_lines_released_amount_positive
        CHECK (released_amount > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_settlement_lines_settlement_batch_item_id
    ON settlement_lines (settlement_batch_item_id);