package com.li.lwg.service;

import com.li.lwg.AbstractIntegrationTest;
import com.li.lwg.dto.UserRechargeReq;
import com.li.lwg.entity.TransactionLog;
import com.li.lwg.entity.User;
import com.li.lwg.exception.ServiceException;
import com.li.lwg.mapper.TransactionLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 用户充值的正确性与并发原子性测试。
 *
 * <p>与改造前的差异（改动原因见 docs/change-log.md）：
 * <ul>
 *   <li>不再连开发者本机数据库：改用 {@link AbstractIntegrationTest} 的一次性容器，
 *       因此重复执行 {@code mvn test} 不会再把十万灵石真实充进开发库；</li>
 *   <li>不再硬编码 {@code userId = 1L}：夹具由 {@code fixtures/recharge-users.sql} 提供固定主键；</li>
 *   <li>并发数由 1000 降到 {@value #THREAD_COUNT}：1000 线程在 2 核 CI Runner 上
 *       会因连接池排队导致耗时与结果不稳定，100 已足以覆盖行锁竞争场景；</li>
 *   <li>补充了金额校验、非法入参、以及「流水条数必须与成功次数一致」的断言。</li>
 * </ul>
 *
 * @author liyuelian
 */
class UserRechargeTest extends AbstractIntegrationTest {

    /** 并发线程数：兼顾行锁竞争覆盖度与 CI 稳定性 */
    private static final int THREAD_COUNT = 100;

    /** 每个线程充值金额 */
    private static final long SINGLE_AMOUNT = 100L;

    /** 夹具中的用户主键，见 src/test/resources/fixtures/recharge-users.sql */
    private static final long USER_ID = 1L;

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionLogMapper transactionLogMapper;

    @Test
    @DisplayName("基础充值：余额精确增加，并写入一条充值流水")
    @Sql("/fixtures/recharge-users.sql")
    void rechargeIncreasesBalanceAndWritesLog() {
        User before = userMapper.selectById(USER_ID);
        assertThat(before).as("夹具用户应当存在").isNotNull();
        long balanceBefore = Objects.requireNonNull(before.getBalance());

        UserRechargeReq req = new UserRechargeReq();
        req.setUserId(USER_ID);
        req.setAmount(500L);
        userService.recharge(req);

        User after = userMapper.selectById(USER_ID);
        assertThat(after).isNotNull();
        assertThat(after.getBalance()).isEqualTo(balanceBefore + 500L);

        List<TransactionLog> logs = transactionLogMapper.selectByUserId(USER_ID);
        assertThat(logs)
                .as("每次充值都必须留下一条流水，用于对账")
                .hasSize(1);
        assertThat(logs.get(0).getAmount()).isEqualTo(500L);
        assertThat(logs.get(0).getBalanceAfter()).isEqualTo(after.getBalance());
    }

    @Test
    @DisplayName("非法金额：0 与负数都必须被拒绝，且不产生流水")
    @Sql("/fixtures/recharge-users.sql")
    void rechargeRejectsNonPositiveAmount() {
        for (long illegal : new long[]{0L, -100L}) {
            UserRechargeReq req = new UserRechargeReq();
            req.setUserId(USER_ID);
            req.setAmount(illegal);

            assertThatThrownBy(() -> userService.recharge(req))
                    .as("金额 %s 应当被拒绝", illegal)
                    .isInstanceOf(ServiceException.class);
        }

        assertThat(transactionLogMapper.selectByUserId(USER_ID)).isEmpty();
        User user = userMapper.selectById(USER_ID);
        assertThat(user).isNotNull();
        assertThat(user.getBalance()).as("被拒绝的充值不得改动余额").isEqualTo(1000L);
    }

    @Test
    @DisplayName("并发充值：" + THREAD_COUNT + " 个线程同时充值，金额与流水条数都必须精确")
    @Sql("/fixtures/recharge-users.sql")
    void concurrentRechargeIsAtomic() throws InterruptedException {
        User initial = userMapper.selectById(USER_ID);
        assertThat(initial).isNotNull();
        long balanceBefore = Objects.requireNonNull(initial.getBalance());
        int versionBefore = Objects.requireNonNull(initial.getVersion());

        AtomicInteger failures = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREAD_COUNT);

        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.execute(() -> {
                    try {
                        startGate.await(); // 所有线程原地待命，尽量制造同时冲击
                        UserRechargeReq req = new UserRechargeReq();
                        req.setUserId(USER_ID);
                        req.setAmount(SINGLE_AMOUNT);
                        userService.recharge(req);
                    } catch (Exception e) {
                        failures.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                });
            }

            startGate.countDown(); // 发令
            assertThat(done.await(60, TimeUnit.SECONDS))
                    .as("并发充值应在 60 秒内全部结束")
                    .isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(failures.get()).as("并发充值不应有线程抛异常").isZero();

        User finalUser = userMapper.selectById(USER_ID);
        assertThat(finalUser).isNotNull();
        long expected = balanceBefore + SINGLE_AMOUNT * THREAD_COUNT;

        // 核心验证一：金额绝对准确（依赖 MySQL 行锁 + 原子 UPDATE）
        assertThat(finalUser.getBalance()).isEqualTo(expected);
        // 核心验证二：版本号精确增加 THREAD_COUNT 次，说明每次更新都独立生效、无丢失更新
        assertThat(finalUser.getVersion()).isEqualTo(versionBefore + THREAD_COUNT);
        // 核心验证三：流水条数与成功次数一致，对账不会少记
        assertThat(transactionLogMapper.selectByUserId(USER_ID))
                .as("每笔成功交易都必须有一条流水")
                .hasSize(THREAD_COUNT);
    }
}
