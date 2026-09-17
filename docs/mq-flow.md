# MQ 链路

> 当前项目只发现 RabbitMQ 使用，主要用于任务结算后异步更新信誉。

## 1. 配置位置

应用配置：

- `src/main/resources/application.yml`

核心配置：

- `spring.rabbitmq.host=127.0.0.1`
- `spring.rabbitmq.port=5672`
- `spring.rabbitmq.publisher-confirm-type=correlated`
- `spring.rabbitmq.publisher-returns=true`
- `spring.rabbitmq.listener.simple.acknowledge-mode=manual`
- `spring.rabbitmq.listener.simple.prefetch=1`

Bean 配置：

- `RabbitConfig.messageConverter`
- `RabbitConfig.missionExchange`
- `RabbitConfig.reputationQueue`
- `RabbitConfig.bindingReputation`

## 2. Exchange、Queue、Routing Key

| 类型 | 名称 |
| --- | --- |
| TopicExchange | `lwg.mission.exchange` |
| Queue | `lwg.reputation.queue` |
| Routing Key | `mission.settled` |

绑定关系：

```text
lwg.mission.exchange
-- mission.settled -->
lwg.reputation.queue
```

## 3. 生产者链路

生产者：

- `MissionServiceImpl.sendMessage`

触发位置：

- `MissionServiceImpl.auditMission`

链路：

```text
MissionServiceImpl.auditMission
-> TransactionSynchronizationManager.registerSynchronization
-> afterCommit
-> MissionServiceImpl.sendMessage
-> RabbitTemplate.convertAndSend
```

说明：

- 只有审核通过分支会发送 MQ。
- 消息在事务提交后发送，避免数据库回滚但消息已发出的不一致。
- 消息体类型为 `MissionSettledMsg`，字段包括 `userId`、`pass`、`difficulty`、`missionId`。

## 4. 消费者链路

消费者：

- `ReputationListener.handleMessage`

监听队列：

- `lwg.reputation.queue`

链路：

```text
ReputationListener.handleMessage
-> ReputationLogMapper.countBySource
-> UserMapper.selectById
-> MissionDifficultyEnum.getByCode
-> UserMapper.updateReputation
-> ReputationLogMapper.insert
-> channel.basicAck
```

核心处理：

- 使用 `sourceId = missionId` 和 `sourceType = MISSION_SETTLED` 做幂等检查。
- 根据任务难度计算信誉变动值。
- 信誉范围限制在 `[0, 12000]`。
- 更新 `t_user.reputation`。
- 写 `t_reputation_log`。
- 成功后手动 ACK。

## 5. 当前风险与待确认

- `application.yml` 开启了 publisher confirm/return，但当前未看到 `RabbitTemplate` 的 ConfirmCallback / ReturnCallback 配置。
- 审核驳回分支未发送 MQ，但 `ReputationListener` 支持 `pass=false` 扣分逻辑，需要确认业务是否需要失败扣信誉。
- 消费失败时当前 `basicNack(deliveryTag, false, false)` 不回队列，没有死信队列配置。
- 幂等依赖 `t_reputation_log` 查询，是否有唯一索引需要结合 DDL 确认。

