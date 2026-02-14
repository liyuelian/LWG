package com.li.lwg.service;

import com.li.lwg.dto.UserRechargeReq;
import com.li.lwg.entity.User;
import com.li.lwg.mapper.TransactionLogMapper;
import com.li.lwg.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class UserRechargeTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TransactionLogMapper transactionLogMapper;

    @Test
    @DisplayName("基础测试：查询用户")
    void testSingleRecharge() {
        Long userId = 1L;
        User user = userMapper.selectById(userId);
        System.out.println(user.getUsername());

    }

    @Test
    @DisplayName("并发测试：1000人同时给用户1 充值，验证原子性")
    void testConcurrentRecharge() throws InterruptedException {
        Long userId = 1L;
        Long singleAmount = 100L; // 每人充 100
        int threadCount = 1000;

        User initial = userMapper.selectById(userId);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    latch.await(); // 所有人原地待命
                    UserRechargeReq req = new UserRechargeReq();
                    req.setUserId(userId);
                    req.setAmount(singleAmount);
                    userService.recharge(req);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    done.countDown();
                }
            });
        }

        latch.countDown(); // 开始
        done.await();      // 等待结束

        User finalUser = userMapper.selectById(userId);
        Long expected = initial.getBalance() + (singleAmount * threadCount);

        System.out.println(">>> 初始余额: " + initial.getBalance());
        System.out.println(">>> 预期余额: " + expected);
        System.out.println(">>> 实际余额: " + finalUser.getBalance());
        System.out.println(">>> 最终版本: " + finalUser.getVersion());

        // 核心验证：金额必须绝对准确，利用了 MySQL 的行锁排队
        assertThat(finalUser.getBalance()).isEqualTo(expected);
        // 版本号也应该准确增加了 threadCount 次
        assertThat(finalUser.getVersion()).isEqualTo(initial.getVersion() + threadCount);
    }
}