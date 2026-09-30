ALTER TABLE idempotency_keys
    ADD COLUMN failure_code VARCHAR(80) NULL
        AFTER status,

    ADD COLUMN updated_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        AFTER created_at;