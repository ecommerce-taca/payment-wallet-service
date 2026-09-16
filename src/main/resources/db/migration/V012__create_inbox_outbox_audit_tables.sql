CREATE TABLE inbox_events (
    id BINARY(16) NOT NULL,
    consumer_name VARCHAR(120) NOT NULL,
    source VARCHAR(120) NOT NULL,
    event_id VARCHAR(160) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload_hash VARCHAR(128) NOT NULL,
    received_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at DATETIME(6) NULL,
    status VARCHAR(40) NOT NULL,
    failure_code VARCHAR(80) NULL,

    CONSTRAINT pk_inbox_events PRIMARY KEY (id),

    CONSTRAINT uk_inbox_events_consumer_source_event
        UNIQUE (consumer_name, source, event_id),

    CONSTRAINT chk_inbox_events_status
        CHECK (status IN (
            'RECEIVED',
            'PROCESSED',
            'FAILED',
            'IGNORED'
        ))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_inbox_events_event_type_received_at
    ON inbox_events (event_type, received_at);

CREATE INDEX idx_inbox_events_status_received_at
    ON inbox_events (status, received_at);


CREATE TABLE outbox_events (
    id BINARY(16) NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id BINARY(16) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload JSON NOT NULL,
    headers JSON NULL,
    occurred_at DATETIME(6) NOT NULL,
    published_at DATETIME(6) NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000) NULL,

    CONSTRAINT pk_outbox_events PRIMARY KEY (id),

    CONSTRAINT chk_outbox_events_retry_count_non_negative
        CHECK (retry_count >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_outbox_events_published_at_occurred_at
    ON outbox_events (published_at, occurred_at);

CREATE INDEX idx_outbox_events_aggregate_type_aggregate_id_occurred_at
    ON outbox_events (aggregate_type, aggregate_id, occurred_at);

CREATE INDEX idx_outbox_events_event_type_occurred_at
    ON outbox_events (event_type, occurred_at);


CREATE TABLE audit_logs (
    id BINARY(16) NOT NULL,
    actor_user_id BINARY(16) NULL,
    actor_type VARCHAR(40) NOT NULL,
    action VARCHAR(120) NOT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_id BINARY(16) NOT NULL,
    reason VARCHAR(500) NULL,
    metadata JSON NULL,
    occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_audit_logs PRIMARY KEY (id),

    CONSTRAINT chk_audit_logs_actor_type
        CHECK (actor_type IN (
            'USER',
            'ADMIN',
            'SYSTEM'
        )),

    CONSTRAINT chk_audit_logs_actor
        CHECK (
            (actor_type = 'SYSTEM' AND actor_user_id IS NULL)
            OR
            (actor_type IN ('USER', 'ADMIN') AND actor_user_id IS NOT NULL)
        )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_audit_logs_target_type_target_id_occurred_at
    ON audit_logs (target_type, target_id, occurred_at);

CREATE INDEX idx_audit_logs_actor_user_id_occurred_at
    ON audit_logs (actor_user_id, occurred_at);

CREATE INDEX idx_audit_logs_action_occurred_at
    ON audit_logs (action, occurred_at);