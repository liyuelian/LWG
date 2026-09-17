package com.li.lwg;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import com.li.lwg.mapper.UserMapper;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * 集成测试基类：为测试临时启动真实的 MySQL 与 RabbitMQ 容器。
 *
 * <p>设计目的：
 * <ul>
 *   <li><b>不污染开发库</b>：测试连的是容器里的一次性 MySQL，开发者的本地
 *       {@code lwg} 库不会被写入（改造前 {@code UserRechargeTest} 会真实充值十万灵石）；</li>
 *   <li><b>CI 可跑</b>：GitHub Runner 自带 Docker，无需任何外部依赖即可 {@code mvn verify}；</li>
 *   <li><b>验证 Flyway 迁移</b>：容器里是全新空库，{@code V1__init_schema.sql} 会真正执行，
 *       因此测试同时起到了「迁移脚本可用性」的回归作用。</li>
 * </ul>
 *
 * <p>容器在静态代码块中启动，同一 JVM 内的所有测试类共用这一组容器，
 * 避免每个测试类都重启一次 MySQL。数据隔离由各测试类自己的夹具负责
 * （见 {@code src/test/resources/fixtures/*.sql}，用固定主键插入，便于断言）。
 *
 * <p>镜像选择：默认 {@code mysql:8.4} / {@code rabbitmq:3.13}（与生产目标一致）。
 * 若本机 Docker Hub 不可达（例如国内网络），用本地已有镜像覆盖即可：
 * <pre>
 *   mvn test -Dlwg.test.mysql.image=arm64v8/mysql:latest \
 *            -Dlwg.test.rabbitmq.image=arm64v8/rabbitmq:3.13.7
 * </pre>
 *
 * @author liyuelian
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    protected static final MySQLContainer<?> MYSQL;
    protected static final RabbitMQContainer RABBITMQ;

    static {
        MYSQL = new MySQLContainer<>(compatibleImage(
                System.getProperty("lwg.test.mysql.image", "mysql:8.4"), "mysql"))
                .withDatabaseName("lwg")
                .withUsername("test")
                .withPassword("test");

        RABBITMQ = new RabbitMQContainer(compatibleImage(
                System.getProperty("lwg.test.rabbitmq.image", "rabbitmq:3.13"), "rabbitmq"));

        MYSQL.start();
        RABBITMQ.start();
    }

    /**
     * 解析镜像名。若使用的不是 Testcontainers 官方镜像名（例如国内网络下改用本地已缓存的
     * {@code arm64v8/mysql:latest}），则显式声明它可作为官方镜像的替代品，
     * 否则 Testcontainers 会以「未经设计的镜像」为由拒绝启动。
     *
     * @param imageName    实际镜像名
     * @param officialName Testcontainers 期望的官方镜像名
     */
    private static DockerImageName compatibleImage(String imageName, String officialName) {
        DockerImageName image = DockerImageName.parse(imageName);
        if (officialName.equals(image.getRepository())) {
            return image;
        }
        return image.asCompatibleSubstituteFor(officialName);
    }

    /**
     * 把容器的真实连接信息注入 Spring 环境，覆盖 application-dev.yml 里的本机默认值。
     *
     * <p>显式设置 {@code spring.flyway.baseline-on-migrate=false}：容器里是全新空库，
     * 应当真正执行 V1 迁移；而 dev profile 为兼容「已有表的开发库」把该开关设成了 true。
     */
    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);

        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
        registry.add("spring.rabbitmq.virtual-host", () -> "/");

        registry.add("spring.flyway.baseline-on-migrate", () -> false);

        // 并发测试会同时打到上百个连接请求，显式放大连接池，
        // 避免把时间耗在 Hikari 默认 10 连接的排队上（排队不违反正确性，但会让耗时不可预期）
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 50);
    }

    /** 子类断言数据时复用；测试夹具由 @Sql 脚本负责插入，不在此处构造实体 */
    @Autowired
    protected UserMapper userMapper;
}
