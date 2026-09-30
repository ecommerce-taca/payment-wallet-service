ALTER TABLE ledger_accounts
DROP CHECK chk_ledger_accounts_account_type;

ALTER TABLE ledger_accounts
DROP CHECK chk_ledger_accounts_shop_account_type;

ALTER TABLE ledger_accounts
    ADD CONSTRAINT chk_ledger_accounts_account_type
        CHECK (
            account_type IN (
                'VNPAY_CLEARING',
                'COD_CLEARING',
                'PLATFORM_COMMISSION',
                'TAX_PAYABLE',
                'SHIPMENT_PAYABLE',
                'SELLER_PENDING',
                'SELLER_AVAILABLE',
                'PAYOUT_CLEARING',
                'REFUND_CLEARING'
            )
        );

ALTER TABLE ledger_accounts
    ADD CONSTRAINT chk_ledger_accounts_shop_account_type
        CHECK (
            (
                owner_type = 'SYSTEM'
                    AND account_type IN (
                        'VNPAY_CLEARING',
                        'COD_CLEARING',
                        'PLATFORM_COMMISSION',
                        'TAX_PAYABLE',
                        'SHIPMENT_PAYABLE',
                        'PAYOUT_CLEARING',
                        'REFUND_CLEARING'
                    )
                )
                OR
            (
                owner_type = 'SHOP'
                    AND account_type IN (
                        'SELLER_PENDING',
                        'SELLER_AVAILABLE'
                    )
                )
            );

INSERT INTO ledger_accounts (
    id,
    account_code,
    account_type,
    owner_type,
    owner_id,
    currency,
    status,
    created_at
)
VALUES (
    UUID_TO_BIN(
        '30000000-0000-0000-0000-000000000007'
    ),
    'SHIPMENT_PAYABLE:VND',
    'SHIPMENT_PAYABLE',
    'SYSTEM',
    NULL,
    'VND',
    'ACTIVE',
    CURRENT_TIMESTAMP(6)
);