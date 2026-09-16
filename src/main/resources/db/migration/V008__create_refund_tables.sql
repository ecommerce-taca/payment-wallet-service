CREATE TABLE refunds (
    id BINARY(16) NOT NULL,
    payment_id BINARY(16) NOT NULL,
    order_id BINARY(16) NULL,
    amount BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(40) NOT NULL,
    provider VARCHAR(40) NULL,
    provider_ref VARCHAR(120) NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    failure_code VARCHAR(80) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,

    CONSTRAINT pk_refunds PRIMARY KEY (id),

    CONSTRAINT fk_refunds_payment_id
        FOREIGN KEY (payment_id)
        REFERENCES payments (id),

    CONSTRAINT uk_refunds_payment_id_idempotency_key
        UNIQUE (payment_id, idempotency_key),

    CONSTRAINT chk_refunds_amount_positive
        CHECK (amount > 0),

    CONSTRAINT chk_refunds_currency_vnd
        CHECK (currency = 'VND'),

    CONSTRAINT chk_refunds_status
        CHECK (status IN (
            'REQUESTED',
            'PROCESSING',
            'SUCCESS',
            'FAILED',
            'CANCELLED'
        )),

    CONSTRAINT chk_refunds_provider
        CHECK (provider IS NULL OR provider IN ('VNPAY')),

    CONSTRAINT chk_refunds_version_non_negative
        CHECK (version >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_refunds_payment_id_created_at
    ON refunds (payment_id, created_at);

CREATE INDEX idx_refunds_status_created_at
    ON refunds (status, created_at);

CREATE INDEX idx_refunds_provider_provider_ref
    ON refunds (provider, provider_ref);


CREATE TABLE refund_allocations (
    id BINARY(16) NOT NULL,
    refund_id BINARY(16) NOT NULL,
    payment_allocation_id BINARY(16) NOT NULL,
    gross_amount BIGINT NOT NULL,
    commission_reversal BIGINT NOT NULL,
    tax_reversal BIGINT NOT NULL,
    seller_reversal BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_refund_allocations PRIMARY KEY (id),

    CONSTRAINT fk_refund_allocations_refund_id
        FOREIGN KEY (refund_id)
        REFERENCES refunds (id),

    CONSTRAINT fk_refund_allocations_payment_allocation_id
        FOREIGN KEY (payment_allocation_id)
        REFERENCES payment_allocations (id),

    CONSTRAINT uk_refund_allocations_refund_id_payment_allocation_id
        UNIQUE (refund_id, payment_allocation_id),

    CONSTRAINT chk_refund_allocations_gross_amount_positive
        CHECK (gross_amount > 0),

    CONSTRAINT chk_refund_allocations_commission_reversal_non_negative
        CHECK (commission_reversal >= 0),

    CONSTRAINT chk_refund_allocations_tax_reversal_non_negative
        CHECK (tax_reversal >= 0),

    CONSTRAINT chk_refund_allocations_seller_reversal_non_negative
        CHECK (seller_reversal >= 0),

    CONSTRAINT chk_refund_allocations_amount_balanced
        CHECK (gross_amount = commission_reversal + tax_reversal + seller_reversal)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_refund_allocations_payment_allocation_id
    ON refund_allocations (payment_allocation_id);