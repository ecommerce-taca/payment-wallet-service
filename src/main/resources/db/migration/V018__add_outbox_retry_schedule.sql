ALTER TABLE outbox_events
    ADD COLUMN next_attempt_at DATETIME(6) NULL AFTER retry_count;

CREATE INDEX idx_outbox_events_publish_retry
    ON outbox_events (
          published_at,
          next_attempt_at,
          retry_count,
          occurred_at
    );