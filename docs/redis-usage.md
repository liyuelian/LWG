# Redis 使用说明

> 当前项目未发现 Redis 实际使用。

## 1. 当前状态

未发现以下依赖或代码：

- `spring-boot-starter-data-redis`
- `RedisTemplate`
- `StringRedisTemplate`
- `@Cacheable` / `@CacheEvict`
- Redis 连接配置

## 2. 可能的后续接入点

如果后续引入 Redis，优先记录以下内容：

- 缓存 key 命名规则。
- 缓存数据结构和 TTL。
- 缓存更新/删除时机。
- 是否允许缓存穿透、击穿、雪崩风险。
- 与数据库事务的一致性策略。

可能接入场景：

- 用户信息缓存：`UserServiceImpl.getUserInfo`。
- 任务列表缓存：`MissionServiceImpl.getMissionList`。
- 防重复提交或接口限流。
- MQ 消费幂等标记。

## 3. 文档维护要求

新增 Redis 后，请同步补充：

- 依赖和配置位置。
- 具体使用类和方法。
- key 示例。
- 数据一致性策略。
- 失效和回源逻辑。

