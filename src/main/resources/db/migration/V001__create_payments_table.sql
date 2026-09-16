CREATE TABLE payments (
    id BINARY(16) NOT NULL,
    checkout_group_id BINARY(16) NOT NULL,
    buyer_user_id BINARY(16) NOT NULL,
    method VARCHAR(30) NOT NULL,
    amount BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(40) NOT NULL,
    captured_amount BIGINT NOT NULL DEFAULT 0,
    refunded_amount BIGINT NOT NULL DEFAULT 0,
    failure_code VARCHAR(80) NULL,
    expires_at DATETIME(6) NULL,
    paid_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_payments PRIMARY KEY (id),

    CONSTRAINT uk_payments_checkout_group_id
        UNIQUE (checkout_group_id),

    CONSTRAINT chk_payments_method
        CHECK (method IN ('VNPAY', 'COD')),

    CONSTRAINT chk_payments_status
        CHECK (status IN (
            'PENDING',
            'PENDING_COD',
            'SUCCESS',
            'FAILED',
            'EXPIRED',
            'PARTIALLY_REFUNDED',
            'REFUNDED'
        )),

    CONSTRAINT chk_payments_amount_positive
        CHECK (amount > 0),

    CONSTRAINT chk_payments_captured_amount_non_negative
        CHECK (captured_amount >= 0),

    CONSTRAINT chk_payments_refunded_amount_non_negative
        CHECK (refunded_amount >= 0),

    CONSTRAINT chk_payments_refunded_not_greater_than_captured
        CHECK (refunded_amount <= captured_amount),

    CONSTRAINT chk_payments_currency_vnd
        CHECK (currency = 'VND')
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_payments_buyer_user_id_created_at
    ON payments (buyer_user_id, created_at);

CREATE INDEX idx_payments_status_expires_at
    ON payments (status, expires_at);

CREATE INDEX idx_payments_method_status_created_at
    ON payments (method, status, created_at);