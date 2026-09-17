package com.li.lwg;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 应用上下文与数据库迁移的冒烟测试。
 *
 * <p>改造前这个测试只有 {@code contextLoads()}，并且强依赖开发者本机的 MySQL 与 RabbitMQ；
 * 现在改为使用 {@link AbstractIntegrationTest} 提供的一次性容器，并顺带验证两件事：
 * <ul>
 *   <li>数据源确实指向测试容器，而不是开发者的本地库（隔离性断言）；</li>
 *   <li>Flyway 在全新空库上真正执行了 V1 迁移（迁移脚本可用性回归）。</li>
 * </ul>
 *
 * @author liyuelian
 */
class LwgApplicationTests extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("应用上下文可加载")
    void contextLoads() {
        assertThat(jdbcTemplate).isNotNull();
    }

    @Test
    @DisplayName("数据源必须指向测试容器，而不是开发者本地库")
    void datasourcePointsToTestContainer() {
        String url = jdbcTemplate.execute(
                (ConnectionCallback<String>) connection -> connection.getMetaData().getURL());

        assertThat(url)
                .as("测试必须连到 Testcontainers 启动的临时库，绝不能连上开发库 lwg")
                .isEqualTo(MYSQL.getJdbcUrl())
                .contains(String.valueOf(MYSQL.getMappedPort(MySQLContainer.MYSQL_PORT)));
    }

    @Test
    @DisplayName("Flyway 在全新库上执行 V1 迁移并建出四张业务表")
    void flywayMigrationCreatesSchema() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1",
                Integer.class);
        assertThat(applied).as("V1__init_schema.sql 应当被真正执行").isEqualTo(1);

        for (String table : new String[]{"t_user", "t_mission", "t_transaction_log", "t_reputation_log"}) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                    Integer.class, table);
            assertThat(count).as("迁移后应当存在表 %s", table).isEqualTo(1);
        }
    }
}
