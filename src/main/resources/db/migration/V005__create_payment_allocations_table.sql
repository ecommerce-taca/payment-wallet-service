CREATE TABLE payment_allocations (
    id BINARY(16) NOT NULL,
    payment_id BINARY(16) NOT NULL,
    order_id BINARY(16) NOT NULL,
    shop_id BINARY(16) NOT NULL,
    wallet_id BINARY(16) NOT NULL,
    gross_amount BIGINT NOT NULL,
    commission_amount BIGINT NOT NULL,
    tax_amount BIGINT NOT NULL,
    seller_net_amount BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    fee_config_id BINARY(16) NOT NULL,
    tax_config_id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_payment_allocations PRIMARY KEY (id),

    CONSTRAINT fk_payment_allocations_payment_id
        FOREIGN KEY (payment_id)
        REFERENCES payments (id),

    CONSTRAINT fk_payment_allocations_wallet_id
        FOREIGN KEY (wallet_id)
        REFERENCES wallets (id),

    CONSTRAINT fk_payment_allocations_fee_config_id
        FOREIGN KEY (fee_config_id)
        REFERENCES fee_configs (id),

    CONSTRAINT fk_payment_allocations_tax_config_id
        FOREIGN KEY (tax_config_id)
        REFERENCES tax_configs (id),

    CONSTRAINT chk_payment_allocations_gross_amount_positive
        CHECK (gross_amount > 0),

    CONSTRAINT chk_payment_allocations_commission_amount_non_negative
        CHECK (commission_amount >= 0),

    CONSTRAINT chk_payment_allocations_tax_amount_non_negative
        CHECK (tax_amount >= 0),

    CONSTRAINT chk_payment_allocations_seller_net_amount_non_negative
        CHECK (seller_net_amount >= 0),

    CONSTRAINT chk_payment_allocations_amount_balanced
        CHECK (gross_amount = commission_amount + tax_amount + seller_net_amount),

    CONSTRAINT chk_payment_allocations_currency_vnd
        CHECK (currency = 'VND')
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_payment_allocations_payment_id
    ON payment_allocations (payment_id);

CREATE INDEX idx_payment_allocations_order_id
    ON payment_allocations (order_id);

CREATE INDEX idx_payment_allocations_shop_id_created_at
    ON payment_allocations (shop_id, created_at);

CREATE INDEX idx_payment_allocations_wallet_id_created_at
    ON payment_allocations (wallet_id, created_at);

CREATE INDEX idx_payment_allocations_fee_config_id
    ON payment_allocations (fee_config_id);

CREATE INDEX idx_payment_allocations_tax_config_id
    ON payment_allocations (tax_config_id);