CREATE TABLE payments (
    id BINARY(16) NOT NULL,
    checkout_group_id BINARY(16) NOT NULL,
    buyer_user_id BINARY(16) NOT NULL,
    method VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    captured_amount BIGINT NOT NULL DEFAULT 0,
    refunded_amount BIGINT NOT NULL DEFAULT 0,
    failure_code VARCHAR(100) NULL,
    expires_at DATETIME(6) NULL,
    paid_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uk_payments_checkout_group_id UNIQUE (checkout_group_id),

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

CREATE TABLE payment_orders (
    id BINARY(16) NOT NULL,
    payment_id BINARY(16) NOT NULL,
    order_id BINARY(16) NOT NULL,
    shop_id BINARY(16) NOT NULL,
    amount BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_payment_orders PRIMARY KEY (id),
    CONSTRAINT uk_payment_orders_payment_id_order_id UNIQUE (payment_id, order_id),

    CONSTRAINT fk_payment_orders_payment_id
        FOREIGN KEY (payment_id)
        REFERENCES payments (id),

    CONSTRAINT chk_payment_orders_amount_positive
        CHECK (amount > 0),

    CONSTRAINT chk_payment_orders_currency_vnd
        CHECK (currency = 'VND')
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_payment_orders_order_id
    ON payment_orders (order_id);

CREATE INDEX idx_payment_orders_shop_id
    ON payment_orders (shop_id);

CREATE TABLE payment_attempts (
    id BINARY(16) NOT NULL,
    payment_id BINARY(16) NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_transaction_ref VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    payment_url_hash CHAR(64) NULL,
    expires_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    failure_code VARCHAR(100) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    pending_payment_id BINARY(16)
        GENERATED ALWAYS AS (
            CASE
                WHEN status = 'PENDING' THEN payment_id
                ELSE NULL
            END
        ) VIRTUAL,

    CONSTRAINT pk_payment_attempts PRIMARY KEY (id),

    CONSTRAINT uk_payment_attempts_provider_transaction_ref
        UNIQUE (provider, provider_transaction_ref),

    CONSTRAINT uk_payment_attempts_one_pending_per_payment
        UNIQUE (pending_payment_id),

    CONSTRAINT fk_payment_attempts_payment_id
        FOREIGN KEY (payment_id)
        REFERENCES payments (id),

    CONSTRAINT chk_payment_attempts_provider
        CHECK (provider IN ('VNPAY')),

    CONSTRAINT chk_payment_attempts_status
        CHECK (status IN (
            'PENDING',
            'SUCCESS',
            'FAILED',
            'EXPIRED'
        ))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_payment_attempts_payment_id_status
    ON payment_attempts (payment_id, status);

CREATE TABLE payment_events (
    id BINARY(16) NOT NULL,
    payment_id BINARY(16) NOT NULL,
    payment_attempt_id BINARY(16) NULL,
    provider VARCHAR(30) NOT NULL,
    provider_event_id VARCHAR(150) NOT NULL,
    provider_transaction_ref VARCHAR(100) NULL,
    provider_response_code VARCHAR(30) NULL,
    provider_transaction_status VARCHAR(30) NULL,
    amount BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    received_at DATETIME(6) NOT NULL,
    applied_at DATETIME(6) NULL,
    status VARCHAR(30) NOT NULL,
    failure_code VARCHAR(100) NULL,

    CONSTRAINT pk_payment_events PRIMARY KEY (id),

    CONSTRAINT uk_payment_events_provider_event_id
        UNIQUE (provider, provider_event_id),

    CONSTRAINT fk_payment_events_payment_id
        FOREIGN KEY (payment_id)
        REFERENCES payments (id),

    CONSTRAINT fk_payment_events_payment_attempt_id
        FOREIGN KEY (payment_attempt_id)
        REFERENCES payment_attempts (id),

    CONSTRAINT chk_payment_events_provider
        CHECK (provider IN ('VNPAY')),

    CONSTRAINT chk_payment_events_amount_positive
        CHECK (amount > 0),

    CONSTRAINT chk_payment_events_currency_vnd
        CHECK (currency = 'VND'),

    CONSTRAINT chk_payment_events_status
        CHECK (status IN (
            'RECEIVED',
            'APPLIED',
            'IGNORED',
            'FAILED'
        ))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_payment_events_payment_id_received_at
    ON payment_events (payment_id, received_at);

CREATE INDEX idx_payment_events_provider_transaction_ref
    ON payment_events (provider, provider_transaction_ref);