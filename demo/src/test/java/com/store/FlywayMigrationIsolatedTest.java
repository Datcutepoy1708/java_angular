package com.store;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FlywayMigrationIsolatedTest {

    private String getDbHost() {
        if (System.getenv("DB_HOST") != null) return System.getenv("DB_HOST");
        return System.getProperty("spring.datasource.host", "127.0.0.1");
    }

    private String getDbPort() {
        if (System.getenv("DB_PORT") != null) return System.getenv("DB_PORT");
        return System.getProperty("spring.datasource.port", "3306");
    }

    private String getDbUser() {
        if (System.getenv("SPRING_DATASOURCE_USERNAME") != null) return System.getenv("SPRING_DATASOURCE_USERNAME");
        if (System.getenv("DB_USERNAME") != null) return System.getenv("DB_USERNAME");
        return System.getProperty("spring.datasource.username", "root");
    }

    private String getDbPassword() {
        if (System.getenv("SPRING_DATASOURCE_PASSWORD") != null) return System.getenv("SPRING_DATASOURCE_PASSWORD");
        if (System.getenv("DB_PASSWORD") != null) return System.getenv("DB_PASSWORD");
        return System.getProperty("spring.datasource.password", "");
    }

    @Test
    @DisplayName("Verify clean Flyway V1 -> V2 execution on isolated UUID blank DB (strict CREATE TABLE, natural keys, idempotency)")
    void testFlywayMigrations_OnIsolatedBlankDatabase() throws Exception {
        String dbUser = getDbUser();
        String dbPass = getDbPassword();
        String host = getDbHost();
        String port = getDbPort();

        // 1. Generate unique UUID database name so concurrent/repeated runs never conflict
        String testDbName = "cs_flyway_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String rootJdbcUrl = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        String testDbJdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + testDbName + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        // 2. Only create our own designated UUID database
        try (Connection conn = DriverManager.getConnection(rootJdbcUrl, dbUser, dbPass);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE `" + testDbName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }

        try {
            // 3. Run Flyway V1 -> V2 on the clean blank database
            Flyway flyway = Flyway.configure()
                    .dataSource(testDbJdbcUrl, dbUser, dbPass)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(false)
                    .validateOnMigrate(true)
                    .load();

            MigrateResult firstRun = flyway.migrate();
            System.out.println("First migrate result on " + testDbName + ": " + firstRun.migrationsExecuted + " migrations executed.");
            assertEquals(2, firstRun.migrationsExecuted, "Expected V1 and V2 to execute on blank database");
            assertTrue(firstRun.success, "First migration run must succeed");

            // 4. Verify tables, reference data, and exact role mappings
            try (Connection conn = DriverManager.getConnection(testDbJdbcUrl, dbUser, dbPass);
                 Statement stmt = conn.createStatement()) {

                // 4.1. Verify roles table has 3 rows
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM `roles`")) {
                    assertTrue(rs.next());
                    assertEquals(3, rs.getInt(1), "Roles should have exactly 3 rows");
                }

                // 4.2. Verify permissions table has 82 rows
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM `permissions`")) {
                    assertTrue(rs.next());
                    assertEquals(82, rs.getInt(1), "Permissions should have exactly 82 rows");
                }

                // 4.3. Verify ROLE_ADMIN has all 82 permissions mapped
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT COUNT(*) FROM `role_permissions` rp " +
                        "JOIN `roles` r ON r.role_id = rp.role_id " +
                        "WHERE r.role_name = 'ROLE_ADMIN'")) {
                    assertTrue(rs.next());
                    assertEquals(82, rs.getInt(1), "ROLE_ADMIN must have all 82 permissions mapped");
                }

                // 4.4. Verify ROLE_STAFF has exactly 26 mapped permissions
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT COUNT(*) FROM `role_permissions` rp " +
                        "JOIN `roles` r ON r.role_id = rp.role_id " +
                        "WHERE r.role_name = 'ROLE_STAFF'")) {
                    assertTrue(rs.next());
                    assertEquals(26, rs.getInt(1), "ROLE_STAFF should have exactly 26 permissions");
                }

                // 4.5. Verify specific operational permissions exist for ROLE_STAFF
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT p.permission_code FROM `role_permissions` rp " +
                        "JOIN `roles` r ON r.role_id = rp.role_id " +
                        "JOIN `permissions` p ON p.permission_id = rp.permission_id " +
                        "WHERE r.role_name = 'ROLE_STAFF' AND p.permission_code IN ('ORDER_UPDATE_STATUS', 'INVENTORY_MANAGE', 'CHAT_RESPOND')")) {
                    Set<String> matchedCodes = new HashSet<>();
                    while (rs.next()) {
                        matchedCodes.add(rs.getString(1));
                    }
                    assertEquals(3, matchedCodes.size(), "Staff permissions ORDER_UPDATE_STATUS, INVENTORY_MANAGE, CHAT_RESPOND must all be mapped");
                }

                // 4.6. Verify ROLE_CUSTOMER has exactly 7 mapped permissions
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT COUNT(*) FROM `role_permissions` rp " +
                        "JOIN `roles` r ON r.role_id = rp.role_id " +
                        "WHERE r.role_name = 'ROLE_CUSTOMER'")) {
                    assertTrue(rs.next());
                    assertEquals(7, rs.getInt(1), "ROLE_CUSTOMER should have exactly 7 permissions");
                }

                // 4.7. Verify chat_bot_rules has 7 rows
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM `chat_bot_rules`")) {
                    assertTrue(rs.next());
                    assertEquals(7, rs.getInt(1), "Chat bot rules should have 7 rows");
                }
            }

            // 5. Run Flyway a second time to verify idempotency and validation
            MigrateResult secondRun = flyway.migrate();
            System.out.println("Second migrate result on " + testDbName + ": " + secondRun.migrationsExecuted + " migrations executed.");
            assertEquals(0, secondRun.migrationsExecuted, "Second run should execute 0 migrations (up-to-date)");
            assertTrue(secondRun.success, "Second migration run must succeed");

            // 6. Validate migrations checksums
            flyway.validate();
            System.out.println("Flyway validate passed successfully on isolated blank database!");

        } finally {
            // 7. Strictly drop ONLY the UUID database created by this specific test execution
            try (Connection conn = DriverManager.getConnection(rootJdbcUrl, dbUser, dbPass);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DROP DATABASE IF EXISTS `" + testDbName + "`");
            }
        }
    }
}
