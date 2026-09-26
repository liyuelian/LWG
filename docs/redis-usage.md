# Redis 使用说明

## 天道碑排行榜

`spring-boot-starter-data-redis` 提供 `StringRedisTemplate`；连接地址在 `application-dev.yml` / `application-prod.yml` 中配置。`RankIndex` 是唯一读写排行榜 Redis 数据的类。

| Key | 类型 | 内容 |
| --- | --- | --- |
| `lwg:rank:v1:reputation` | ZSET | 正常账号 ID → 当前信誉，NULL 按 6000 |
| `lwg:rank:v1:completed` | ZSET | 至少完成一单的正常账号 ID → 已验收任务数 |
| `lwg:rank:v1:ready` | String | 两张榜单已完成初始化；空榜也用它标记 |

成员是 `Long.MAX_VALUE - userId` 的 19 位补零字符串。Redis 降序读取同分成员时，反转后的值使较小的用户 ID 先出现。分数仅使用整数且须处于 double 可精确表示的范围（`2^53` 以下）。无 TTL：排行榜是长期投影；Redis 内存淘汰策略在 Compose 中设为 `noeviction`，避免只丢部分榜单成员。

`RankIndex.rebuild` 启动时从 MySQL 按 ID 每次 1000 行读取信誉和已完成任务聚合，写入临时 ZSET，最后用 Lua 原子替换两张榜及 ready 标记。若 Redis 键被清空，下一次查询会重新建榜。每天上海时间 03:00 校准一次，修复增量写入失败或非应用途径改动的数据。MySQL 是事实来源，Redis 可以重建；不会对整个 Redis 执行 `FLUSHALL`。

## 查询与同步

`RankServiceImpl.getBoard` 使用 Redis `ZREVRANGE WITHSCORES` 获取前 N 名、`ZSCORE` 查个人分数、`ZCOUNT` 计算严格高于该分数的人数，从而实现 1、1、3 的并列排名；`ZCARD` 返回参与人数。只对展示的最多 100 位用户执行 `RankMapper.selectProfiles` 查询道号和境界。正常请求不在 MySQL 聚合所有任务或使用窗口函数。

- `MissionServiceImpl.auditMission` 审核通过并提交 MySQL 事务后调用 `RankIndex.refreshCompleted`，重新读取该接单者的已完成任务数并写 ZSET。
- `ReputationListener.handleMessage` 更新信誉并提交事务后调用 `RankIndex.refreshReputation`，重新读取该用户的信誉并写 ZSET。
- 增量操作写数据库的最终值，重复调用不会重复累加。Redis 写入失败会记日志，不将已成功提交的任务结算报告为失败；下次每日重建会校正。
- 单实例内的重建和增量刷新使用同一把锁，避免建榜时丢掉刚提交的更新。当前未做跨应用实例的分布式锁；部署多个后端实例前需补充跨实例协调。
- 榜单读取时 Redis 分数与 MySQL 用户展示信息可能短暂不一致。现有用户状态没有独立变更接口；如果后续加入封禁/解禁或手动改信誉，应在提交后调用对应刷新方法。

## 本地与测试

本地启动 `redis` 容器，默认 `127.0.0.1:6379`。生产 Compose 新增内部 `lwg-redis` 服务，开启 AOF，数据卷 `lwg-redis-data`，后端等待其健康。

`AbstractIntegrationTest` 增加一次性 Redis 容器，测试可用镜像参数 `-Dlwg.test.redis.image=arm64v8/redis:latest` 覆盖默认 `redis:7`。测试只清理榜单测试容器中的对应 key，不碰开发 Redis 数据。
