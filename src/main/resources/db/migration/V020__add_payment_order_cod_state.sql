ALTER TABLE payment_orders
    ADD COLUMN cod_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN cod_processed_at DATETIME(6) NULL,
    ADD COLUMN cod_failure_code VARCHAR(80) NULL;

ALTER TABLE payment_orders
    ADD CONSTRAINT chk_payment_orders_cod_status
        CHECK (cod_status IN ('PENDING', 'CAPTURED', 'FAILED'));

ALTER TABLE payment_orders
    ADD CONSTRAINT chk_payment_orders_cod_state
        CHECK (
            (cod_status = 'PENDING' AND cod_processed_at IS NULL AND cod_failure_code IS NULL)
            OR
            (cod_status = 'CAPTURED' AND cod_processed_at IS NOT NULL AND cod_failure_code IS NULL)
            OR
            (cod_status = 'FAILED' AND cod_processed_at IS NOT NULL AND cod_failure_code IS NOT NULL)
        );

ALTER TABLE payment_orders
    ADD CONSTRAINT uk_payment_orders_order_id UNIQUE (order_id);

ALTER TABLE payment_allocations
    ADD CONSTRAINT uk_payment_allocations_payment_order UNIQUE (payment_id, order_id);

CREATE INDEX idx_payment_orders_cod_status ON payment_orders (cod_status);