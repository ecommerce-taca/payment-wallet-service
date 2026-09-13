package com.taca.paymentwallet.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.0")
            .withDatabaseName("payment_wallet_test")
            .withUsername("test")
            .withPassword("test");

    @Test
    void shouldRunFlywayMigrationsFromEmptyDatabase() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        MYSQL.getJdbcUrl(),
                        MYSQL.getUsername(),
                        MYSQL.getPassword()
                )
                .locations("classpath:db/migration")
                .load();

        MigrateResult result = flyway.migrate();

        assertThat(result.success).isTrue();
        assertThat(tableExists(flyway, "payments")).isTrue();
        assertThat(tableExists(flyway, "payment_orders")).isTrue();
        assertThat(tableExists(flyway, "payment_attempts")).isTrue();
        assertThat(tableExists(flyway, "payment_events")).isTrue();
        assertThat(tableExists(flyway, "wallets")).isTrue();
    }

    private boolean tableExists(Flyway flyway, String tableName) throws Exception {
        try (Connection connection = flyway.getConfiguration()
                .getDataSource()
                .getConnection();
             ResultSet resultSet = connection.getMetaData()
                     .getTables(null, null, tableName, new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }
}