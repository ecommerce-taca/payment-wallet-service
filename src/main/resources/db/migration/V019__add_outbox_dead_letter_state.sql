ALTER TABLE outbox_events
    ADD COLUMN dead_lettered_at DATETIME(6) NULL AFTER next_attempt_at;

CREATE INDEX idx_outbox_events_dead_letter
    ON outbox_events (
        published_at,
        dead_lettered_at,
        retry_count,
        occurred_at
    );