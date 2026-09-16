SET @admin_id = UUID_TO_BIN('00000000-0000-0000-0000-000000000001');

SET @fee_config_id = UUID_TO_BIN('10000000-0000-0000-0000-000000000001');
SET @tax_config_id = UUID_TO_BIN('10000000-0000-0000-0000-000000000002');

SET @shop_1_id = UUID_TO_BIN('20000000-0000-0000-0000-000000000001');
SET @shop_2_id = UUID_TO_BIN('20000000-0000-0000-0000-000000000002');

SET @wallet_1_id = UUID_TO_BIN('21000000-0000-0000-0000-000000000001');
SET @wallet_2_id = UUID_TO_BIN('21000000-0000-0000-0000-000000000002');

SET @vnpay_clearing_account_id = UUID_TO_BIN('30000000-0000-0000-0000-000000000001');
SET @cod_clearing_account_id = UUID_TO_BIN('30000000-0000-0000-0000-000000000002');
SET @platform_commission_account_id = UUID_TO_BIN('30000000-0000-0000-0000-000000000003');
SET @tax_payable_account_id = UUID_TO_BIN('30000000-0000-0000-0000-000000000004');
SET @payout_clearing_account_id = UUID_TO_BIN('30000000-0000-0000-0000-000000000005');
SET @refund_clearing_account_id = UUID_TO_BIN('30000000-0000-0000-0000-000000000006');

SET @seller_1_pending_account_id = UUID_TO_BIN('31000000-0000-0000-0000-000000000001');
SET @seller_1_available_account_id = UUID_TO_BIN('31000000-0000-0000-0000-000000000002');
SET @seller_2_pending_account_id = UUID_TO_BIN('31000000-0000-0000-0000-000000000003');
SET @seller_2_available_account_id = UUID_TO_BIN('31000000-0000-0000-0000-000000000004');

INSERT INTO fee_configs (
    id, scope, category_id, rate_bps, effective_from, note, created_by, created_at
) VALUES (
    @fee_config_id, 'PLATFORM', NULL, 700,
    '2026-01-01 00:00:00.000000',
    'Local/test platform commission 7%',
    @admin_id,
    CURRENT_TIMESTAMP(6)
);

INSERT INTO tax_configs (
    id, scope, category_id, rate_bps, effective_from, note, created_by, created_at
) VALUES (
    @tax_config_id, 'PLATFORM', NULL, 300,
    '2026-01-01 00:00:00.000000',
    'Local/test tax 3%',
    @admin_id,
    CURRENT_TIMESTAMP(6)
 );

INSERT INTO wallets (
    id, shop_id, currency, available_balance, pending_balance, status, version, created_at, updated_at
) VALUES
      (@wallet_1_id, @shop_1_id, 'VND', 31000, 0, 'ACTIVE', 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
      (@wallet_2_id, @shop_2_id, 'VND', 0, 135000, 'ACTIVE', 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

INSERT INTO ledger_accounts (
    id, account_code, account_type, owner_type, owner_id, currency, status, created_at
) VALUES
    (@vnpay_clearing_account_id, 'VNPAY_CLEARING:VND', 'VNPAY_CLEARING', 'SYSTEM', NULL, 'VND', 'ACTIVE', CURRENT_TIMESTAMP(6)),
    (@cod_clearing_account_id, 'COD_CLEARING:VND', 'COD_CLEARING', 'SYSTEM', NULL, 'VND', 'ACTIVE', CURRENT_TIMESTAMP(6)),
    (@platform_commission_account_id, 'PLATFORM_COMMISSION:VND', 'PLATFORM_COMMISSION', 'SYSTEM', NULL, 'VND', 'ACTIVE', CURRENT_TIMESTAMP(6)),
    (@tax_payable_account_id, 'TAX_PAYABLE:VND', 'TAX_PAYABLE', 'SYSTEM', NULL, 'VND', 'ACTIVE', CURRENT_TIMESTAMP(6)),
    (@payout_clearing_account_id, 'PAYOUT_CLEARING:VND', 'PAYOUT_CLEARING', 'SYSTEM', NULL, 'VND', 'ACTIVE', CURRENT_TIMESTAMP(6)),
    (@refund_clearing_account_id, 'REFUND_CLEARING:VND', 'REFUND_CLEARING', 'SYSTEM', NULL, 'VND', 'ACTIVE', CURRENT_TIMESTAMP(6)),
    (@seller_1_pending_account_id, 'SELLER_PENDING:20000000-0000-0000-0000-000000000001:VND', 'SELLER_PENDING', 'SHOP', @shop_1_id, 'VND', 'ACTIVE', CURRENT_TIMESTAMP(6)),
    (@seller_1_available_account_id, 'SELLER_AVAILABLE:20000000-0000-0000-0000-000000000001:VND', 'SELLER_AVAILABLE', 'SHOP', @shop_1_id, 'VND', 'ACTIVE', CURRENT_TIMESTAMP(6)),
    (@seller_2_pending_account_id, 'SELLER_PENDING:20000000-0000-0000-0000-000000000002:VND', 'SELLER_PENDING', 'SHOP', @shop_2_id, 'VND', 'ACTIVE', CURRENT_TIMESTAMP(6)),
    (@seller_2_available_account_id, 'SELLER_AVAILABLE:20000000-0000-0000-0000-000000000002:VND', 'SELLER_AVAILABLE', 'SHOP', @shop_2_id, 'VND', 'ACTIVE', CURRENT_TIMESTAMP(6));

INSERT INTO inbox_events (
    id, consumer_name, source, event_id, event_type, payload_hash, received_at, processed_at, status, failure_code
) VALUES (
    UUID_TO_BIN('90000000-0000-0000-0000-000000000001'),
    'payment-wallet-shipment-consumer',
    'shipment-service',
    'shipment-event-local-001',
    'shipment.delivered',
    SHA2('shipment-event-local-001', 256),
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    'PROCESSED',
    NULL
);

INSERT INTO outbox_events (
    id, aggregate_type, aggregate_id, event_type, payload, headers, occurred_at, published_at, retry_count, last_error
) VALUES
    (
        UUID_TO_BIN('91000000-0000-0000-0000-000000000001'),
        'PAYMENT',
        UUID_TO_BIN('40000000-0000-0000-0000-000000000001'),
        'payment.succeeded',
        JSON_OBJECT('paymentId', '40000000-0000-0000-0000-000000000001', 'amount', 100000, 'currency', 'VND'),
        JSON_OBJECT('traceId', 'local-trace-001'),
        CURRENT_TIMESTAMP(6),
        NULL,
        0,
        NULL
    ),
    (
        UUID_TO_BIN('91000000-0000-0000-0000-000000000002'),
        'PAYOUT',
        UUID_TO_BIN('70000000-0000-0000-0000-000000000001'),
        'payout.succeeded',
        JSON_OBJECT('payoutId', '70000000-0000-0000-0000-000000000001', 'amount', 50000, 'currency', 'VND'),
        JSON_OBJECT('traceId', 'local-trace-002'),
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6),
        0,
        NULL
    );

INSERT INTO audit_logs (
    id, actor_user_id, actor_type, action, target_type, target_id, reason, metadata, occurred_at
) VALUES (
    UUID_TO_BIN('92000000-0000-0000-0000-000000000001'),
    NULL,
    'SYSTEM',
    'SEED_LOCAL_TEST_DATA',
    'DATABASE',
    UUID_TO_BIN('00000000-0000-0000-0000-000000000000'),
    'Seed local/test data for payment-wallet-service',
    JSON_OBJECT('migration', 'V014'),
    CURRENT_TIMESTAMP(6)
);