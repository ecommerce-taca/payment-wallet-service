CREATE TABLE payment_orders (
    id BINARY(16) NOT NULL,
    payment_id BINARY(16) NOT NULL,
    order_id BINARY(16) NOT NULL,
    shop_id BINARY(16) NOT NULL,
    amount BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_payment_orders PRIMARY KEY (id),

    CONSTRAINT fk_payment_orders_payment_id
        FOREIGN KEY (payment_id)
        REFERENCES payments (id),

    CONSTRAINT uk_payment_orders_payment_id_order_id
        UNIQUE (payment_id, order_id),

    CONSTRAINT chk_payment_orders_amount_positive
        CHECK (amount > 0),

    CONSTRAINT chk_payment_orders_currency_vnd
        CHECK (currency = 'VND')
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_payment_orders_order_id
    ON payment_orders (order_id);

CREATE INDEX idx_payment_orders_shop_id_created_at
    ON payment_orders (shop_id, created_at);


CREATE TABLE payment_attempts (
    id BINARY(16) NOT NULL,
    payment_id BINARY(16) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    provider_transaction_ref VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    payment_url_hash CHAR(64) NULL,
    expires_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    failure_code VARCHAR(80) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_payment_attempts PRIMARY KEY (id),

    CONSTRAINT fk_payment_attempts_payment_id
        FOREIGN KEY (payment_id)
        REFERENCES payments (id),

    CONSTRAINT uk_payment_attempts_provider_transaction_ref
        UNIQUE (provider, provider_transaction_ref),

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

CREATE INDEX idx_payment_attempts_payment_id_created_at
    ON payment_attempts (payment_id, created_at);

CREATE INDEX idx_payment_attempts_status_expires_at
    ON payment_attempts (status, expires_at);


CREATE TABLE payment_events (
    id BINARY(16) NOT NULL,
    payment_id BINARY(16) NULL,
    payment_attempt_id BINARY(16) NULL,
    provider VARCHAR(40) NOT NULL,
    provider_event_id VARCHAR(120) NOT NULL,
    provider_transaction_ref VARCHAR(120) NULL,
    provider_response_code VARCHAR(40) NULL,
    provider_transaction_status VARCHAR(40) NULL,
    amount BIGINT NULL,
    currency CHAR(3) NULL,
    payload_hash CHAR(64) NOT NULL,
    received_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    applied_at DATETIME(6) NULL,
    status VARCHAR(40) NOT NULL,
    failure_code VARCHAR(80) NULL,

    CONSTRAINT pk_payment_events PRIMARY KEY (id),

    CONSTRAINT fk_payment_events_payment_id
        FOREIGN KEY (payment_id)
        REFERENCES payments (id),

    CONSTRAINT fk_payment_events_payment_attempt_id
        FOREIGN KEY (payment_attempt_id)
        REFERENCES payment_attempts (id),

    CONSTRAINT uk_payment_events_provider_event_id
        UNIQUE (provider, provider_event_id),

    CONSTRAINT chk_payment_events_provider
        CHECK (provider IN ('VNPAY')),

    CONSTRAINT chk_payment_events_status
        CHECK (status IN (
            'RECEIVED',
            'APPLIED',
            'IGNORED',
            'FAILED'
        )),

    CONSTRAINT chk_payment_events_amount_positive
        CHECK (amount IS NULL OR amount > 0),

    CONSTRAINT chk_payment_events_currency_vnd
        CHECK (currency IS NULL OR currency = 'VND')
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_payment_events_payment_id_received_at
    ON payment_events (payment_id, received_at);

CREATE INDEX idx_payment_events_payment_attempt_id_received_at
    ON payment_events (payment_attempt_id, received_at);

CREATE INDEX idx_payment_events_provider_transaction_ref
    ON payment_events (provider, provider_transaction_ref);