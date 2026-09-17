-- ============================================================================
-- 测试夹具：两个用于充值验证的修士
--
-- 使用固定主键（1 / 2），这样测试可以直接断言，不依赖自增 ID 的取值。
--
-- 本脚本由 @Sql 在每个测试方法执行前加载，因此必须是幂等的：
-- 先按主键删除，再插入。容器库虽是一次性的，但同一个测试类内的多个方法
-- 会共享同一个库，缺少 DELETE 会导致第二个方法撞主键约束。
--
-- 顺带清理这两个用户的流水，避免跨方法残留影响「流水条数」类断言。
-- ============================================================================

DELETE FROM t_transaction_log WHERE user_id IN (1, 2);
DELETE FROM t_user WHERE id IN (1, 2);

INSERT INTO t_user (id, username, password, realm, balance, frozen_balance, status, version, reputation)
VALUES (1, '测试_并发充值甲', 'test-password-hash', 1, 1000, 0, 1, 0, 6000),
       (2, '测试_并发充值乙', 'test-password-hash', 5, 0, 0, 1, 0, 6000);
