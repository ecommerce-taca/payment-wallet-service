CREATE INDEX idx_outbox_events_cleanup_published
    ON outbox_events (
        published_at,
        occurred_at
    );

CREATE INDEX idx_outbox_events_cleanup_dead_lettered
    ON outbox_events (
        dead_lettered_at,
        occurred_at
    );