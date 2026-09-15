CREATE TABLE idempotency_keys (
    id BINARY(16) NOT NULL,
    scope VARCHAR(60) NOT NULL,
    scope_id VARCHAR(160) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    response_snapshot JSON NULL,
    status VARCHAR(40) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_idempotency_keys PRIMARY KEY (id),

    CONSTRAINT uk_idempotency_keys_scope_scope_id_key
        UNIQUE (scope, scope_id, idempotency_key),

    CONSTRAINT chk_idempotency_keys_scope
        CHECK (scope IN (
            'PAYMENT',
            'REFUND',
            'PAYOUT'
        )),

    CONSTRAINT chk_idempotency_keys_status
        CHECK (status IN (
            'PROCESSING',
            'SUCCEEDED',
            'FAILED'
        )),

    CONSTRAINT chk_idempotency_keys_expires_at_after_created_at
        CHECK (expires_at > created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_idempotency_keys_expires_at
    ON idempotency_keys (expires_at);