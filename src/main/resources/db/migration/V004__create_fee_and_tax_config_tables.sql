CREATE TABLE fee_configs (
    id BINARY(16) NOT NULL,
    scope VARCHAR(40) NOT NULL,
    category_id BINARY(16) NULL,
    rate_bps INT NOT NULL,
    effective_from DATETIME(6) NOT NULL,
    note VARCHAR(500) NULL,
    created_by BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_fee_configs PRIMARY KEY (id),

    CONSTRAINT chk_fee_configs_scope
        CHECK (scope IN ('PLATFORM', 'CATEGORY')),

    CONSTRAINT chk_fee_configs_rate_bps
        CHECK (rate_bps >= 0 AND rate_bps <= 10000),

    CONSTRAINT chk_fee_configs_scope_category
        CHECK (
            (scope = 'PLATFORM' AND category_id IS NULL)
            OR
            (scope = 'CATEGORY' AND category_id IS NOT NULL)
        )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_fee_configs_scope_category_effective_from
    ON fee_configs (scope, category_id, effective_from);


CREATE TABLE tax_configs (
    id BINARY(16) NOT NULL,
    scope VARCHAR(40) NOT NULL,
    category_id BINARY(16) NULL,
    rate_bps INT NOT NULL,
    effective_from DATETIME(6) NOT NULL,
    note VARCHAR(500) NULL,
    created_by BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_tax_configs PRIMARY KEY (id),

    CONSTRAINT chk_tax_configs_scope
        CHECK (scope IN ('PLATFORM', 'CATEGORY')),

    CONSTRAINT chk_tax_configs_rate_bps
        CHECK (rate_bps >= 0 AND rate_bps <= 10000),

    CONSTRAINT chk_tax_configs_scope_category
        CHECK (
            (scope = 'PLATFORM' AND category_id IS NULL)
            OR
            (scope = 'CATEGORY' AND category_id IS NOT NULL)
        )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_tax_configs_scope_category_effective_from
    ON tax_configs (scope, category_id, effective_from);