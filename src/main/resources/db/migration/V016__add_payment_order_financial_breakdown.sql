ALTER TABLE payment_orders
    ADD COLUMN merchandise_amount BIGINT NULL
        AFTER amount,
    ADD COLUMN shipping_fee BIGINT NOT NULL DEFAULT 0
        AFTER merchandise_amount;

UPDATE payment_orders
SET merchandise_amount = amount
WHERE merchandise_amount IS NULL;

ALTER TABLE payment_orders
    MODIFY COLUMN merchandise_amount BIGINT NOT NULL;

ALTER TABLE payment_orders
    ADD CONSTRAINT chk_payment_orders_merchandise_amount_positive
        CHECK (merchandise_amount > 0),
    ADD CONSTRAINT chk_payment_orders_shipping_fee_non_negative
        CHECK (shipping_fee >= 0),
    ADD CONSTRAINT chk_payment_orders_total_amount
        CHECK (
            amount = merchandise_amount + shipping_fee
        );