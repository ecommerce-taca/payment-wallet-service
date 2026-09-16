package com.taca.paymentwallet.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class DatabaseSchemaContractTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.0")
            .withDatabaseName("payment_wallet_contract_test")
            .withUsername("test")
            .withPassword("test");

    private static Flyway flyway;

    @BeforeAll
    static void migrate() {
        flyway = Flyway.configure()
                .dataSource(
                        MYSQL.getJdbcUrl(),
                        MYSQL.getUsername(),
                        MYSQL.getPassword()
                )
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();
    }

    @Test
    void shouldCreateAllRequiredTables() throws Exception {
        assertTableExists("payments");
        assertTableExists("payment_orders");
        assertTableExists("payment_attempts");
        assertTableExists("payment_events");
        assertTableExists("wallets");
        assertTableExists("fee_configs");
        assertTableExists("tax_configs");
        assertTableExists("payment_allocations");
        assertTableExists("ledger_accounts");
        assertTableExists("ledger_postings");
        assertTableExists("ledger_entries");
        assertTableExists("refunds");
        assertTableExists("refund_allocations");
        assertTableExists("payouts");
        assertTableExists("settlement_batches");
        assertTableExists("settlement_batch_items");
        assertTableExists("settlement_lines");
        assertTableExists("idempotency_keys");
        assertTableExists("inbox_events");
        assertTableExists("outbox_events");
        assertTableExists("audit_logs");
    }

    @Test
    void paymentsShouldMatchContract() throws Exception {
        assertColumnExists("payments", "checkout_group_id");
        assertColumnExists("payments", "buyer_user_id");
        assertColumnExists("payments", "captured_amount");
        assertColumnExists("payments", "refunded_amount");
        assertIndexExists("payments", "uk_payments_checkout_group_id");
    }

    @Test
    void paymentEventsShouldAllowUnmatchedProviderEvent() throws Exception {
        assertColumnNullable("payment_events", "payment_id");
        assertColumnNullable("payment_events", "payment_attempt_id");
        assertIndexExists("payment_events", "uk_payment_events_provider_event_id");
    }

    @Test
    void walletsShouldUseFrozenStatusContract() throws Exception {
        assertColumnExists("wallets", "available_balance");
        assertColumnExists("wallets", "pending_balance");
        assertColumnExists("wallets", "version");
        assertIndexExists("wallets", "uk_wallets_shop_id_currency");
    }

    @Test
    void paymentAllocationsShouldSnapshotFeeAndTaxConfig() throws Exception {
        assertColumnExists("payment_allocations", "fee_config_id");
        assertColumnExists("payment_allocations", "tax_config_id");
        assertColumnDoesNotExist("payment_allocations", "payment_order_id");
    }

    @Test
    void ledgerEntriesShouldReferenceLedgerAccountOnly() throws Exception {
        assertColumnExists("ledger_entries", "posting_id");
        assertColumnExists("ledger_entries", "account_id");
        assertColumnDoesNotExist("ledger_entries", "wallet_id");
        assertColumnDoesNotExist("ledger_entries", "balance_after");
    }

    @Test
    void refundsShouldContainOrderReferenceAndProviderFields() throws Exception {
        assertColumnExists("refunds", "order_id");
        assertColumnExists("refunds", "provider");
        assertColumnExists("refunds", "provider_ref");
        assertColumnExists("refunds", "completed_at");
        assertIndexExists("refunds", "uk_refunds_payment_id_idempotency_key");
    }

    @Test
    void payoutsShouldUseBankAccountSnapshot() throws Exception {
        assertColumnExists("payouts", "bank_account_snapshot");
        assertColumnExists("payouts", "provider");
        assertColumnExists("payouts", "provider_ref");
        assertColumnExists("payouts", "completed_at");
        assertColumnDoesNotExist("payouts", "bank_account_number");
        assertIndexExists("payouts", "uk_payouts_shop_id_idempotency_key");
    }

    @Test
    void settlementLinesShouldPreventDoubleSettlement() throws Exception {
        assertColumnExists("settlement_lines", "payment_allocation_id");
        assertIndexExists("settlement_lines", "uk_settlement_lines_payment_allocation_id");
    }

    @Test
    void inboxOutboxAndAuditShouldMatchContract() throws Exception {
        assertIndexExists("inbox_events", "uk_inbox_events_consumer_source_event");
        assertColumnExists("outbox_events", "payload");
        assertColumnExists("outbox_events", "headers");
        assertColumnExists("audit_logs", "metadata");
    }

    private void assertTableExists(String tableName) throws Exception {
        assertThat(tableExists(tableName))
                .as("table %s should exist", tableName)
                .isTrue();
    }

    private void assertColumnExists(String tableName, String columnName) throws Exception {
        assertThat(columnExists(tableName, columnName))
                .as("column %s.%s should exist", tableName, columnName)
                .isTrue();
    }

    private void assertColumnDoesNotExist(String tableName, String columnName) throws Exception {
        assertThat(columnExists(tableName, columnName))
                .as("column %s.%s should not exist", tableName, columnName)
                .isFalse();
    }

    private void assertColumnNullable(String tableName, String columnName) throws Exception {
        try (Connection connection = connection();
             ResultSet resultSet = connection.getMetaData()
                     .getColumns(null, null, tableName, columnName)) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt("NULLABLE"))
                    .as("column %s.%s should be nullable", tableName, columnName)
                    .isEqualTo(ResultSetMetaDataColumnNullable.NULLABLE);
        }
    }

    private void assertIndexExists(String tableName, String indexName) throws Exception {
        assertThat(indexExists(tableName, indexName))
                .as("index %s.%s should exist", tableName, indexName)
                .isTrue();
    }

    private boolean tableExists(String tableName) throws Exception {
        try (Connection connection = connection();
             ResultSet resultSet = connection.getMetaData()
                     .getTables(null, null, tableName, new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }

    private boolean columnExists(String tableName, String columnName) throws Exception {
        try (Connection connection = connection();
             ResultSet resultSet = connection.getMetaData()
                     .getColumns(null, null, tableName, columnName)) {
            return resultSet.next();
        }
    }

    private boolean indexExists(String tableName, String indexName) throws Exception {
        try (Connection connection = connection();
             ResultSet resultSet = connection.getMetaData()
                     .getIndexInfo(null, null, tableName, false, false)) {
            while (resultSet.next()) {
                if (indexName.equals(resultSet.getString("INDEX_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private Connection connection() throws SQLException {
        return flyway.getConfiguration()
                .getDataSource()
                .getConnection();
    }

    private static final class ResultSetMetaDataColumnNullable {
        private static final int NULLABLE = 1;

        private ResultSetMetaDataColumnNullable() {
        }
    }
}