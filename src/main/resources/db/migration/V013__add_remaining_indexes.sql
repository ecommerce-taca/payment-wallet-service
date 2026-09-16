CREATE INDEX idx_payments_status_created_at
    ON payments (status, created_at);

CREATE INDEX idx_payments_paid_at
    ON payments (paid_at);

CREATE INDEX idx_payment_attempts_payment_id_status_created_at
    ON payment_attempts (payment_id, status, created_at);

CREATE INDEX idx_payment_events_status_received_at
    ON payment_events (status, received_at);

CREATE INDEX idx_payment_events_provider_received_at
    ON payment_events (provider, received_at);

CREATE INDEX idx_payment_allocations_payment_id_shop_id
    ON payment_allocations (payment_id, shop_id);

CREATE INDEX idx_refunds_status_updated_at
    ON refunds (status, updated_at);

CREATE INDEX idx_refunds_provider_created_at
    ON refunds (provider, created_at);

CREATE INDEX idx_payouts_status_updated_at
    ON payouts (status, updated_at);

CREATE INDEX idx_payouts_provider_requested_at
    ON payouts (provider, requested_at);

CREATE INDEX idx_settlement_batch_items_status_created_at
    ON settlement_batch_items (status, created_at);

CREATE INDEX idx_idempotency_keys_status_created_at
    ON idempotency_keys (status, created_at);

CREATE INDEX idx_inbox_events_status_event_type_received_at
    ON inbox_events (status, event_type, received_at);

CREATE INDEX idx_outbox_events_published_retry_occurred_at
    ON outbox_events (published_at, retry_count, occurred_at);

CREATE INDEX idx_audit_logs_occurred_at
    ON audit_logs (occurred_at);