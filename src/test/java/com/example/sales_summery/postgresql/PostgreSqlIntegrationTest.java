package com.example.sales_summery.postgresql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Docker가 있는 환경에서 실제 PostgreSQL에 Flyway 스키마가 적용되는지 검증한다.

// Docker가 있는 환경에서는 실제 PostgreSQL에 Flyway와 핵심 쿼리를 검증한다.
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=test-only-jwt-secret-key-must-be-at-least-32-bytes"
})
class PostgreSqlIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesSchemaOnPostgreSql() throws Exception {
        String productName = jdbcTemplate.getDataSource().getConnection()
                .getMetaData().getDatabaseProductName();
        Integer migrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE", Integer.class);

        assertThat(productName).isEqualTo("PostgreSQL");
        assertThat(migrations).isPositive();
    }
}
