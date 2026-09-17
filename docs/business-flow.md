# 核心业务链路

> 本文按“入口 -> 编排 -> 核心处理 -> 持久化/消息/下游”的方式记录主链路。

## 1. 任务发布

入口：

- `POST /api/mission/publish`
- `MissionController.publish`

链路：

```text
MissionController.publish
-> MissionServiceImpl.publishMission
-> UserMapper.freezeBalance
-> MissionMapper.insert
-> TransactionLogMapper.insert
```

核心处理：

- 校验 `MissionPublishReq.reward` 必须大于 0。
- `UserMapper.freezeBalance` 扣减发布者可用余额并增加冻结余额。
- `MissionMapper.insert` 创建 `t_mission` 任务，初始状态为待接单。
- `TransactionLogMapper.insert` 写两条流水：可用余额减少、冻结余额增加。

## 2. 任务抢单

入口：

- `POST /api/mission/accept`
- `MissionController.accept`

链路：

```text
MissionController.accept
-> MissionServiceImpl.acceptMission
-> MissionMapper.selectById
-> UserMapper.selectById
-> MissionMapper.acceptMission
```

核心处理：

- 校验任务存在且 `status = 0`。
- 校验接单人不是发布者本人。
- 校验接单人 `realm` 满足任务 `minRealm`。
- `MissionMapper.acceptMission` 使用 `version` 乐观锁更新任务为进行中。

## 3. 任务提交

入口：

- `POST /api/mission/submit`
- `MissionController.submit`

链路：

```text
MissionController.submit
-> MissionServiceImpl.submitMission
-> ObjectMapper.writeValueAsString
-> MissionMapper.submitMission
```

核心处理：

- 校验 `MissionSubmitReq.proofData` 不为空。
- 将提交凭证序列化为 JSON 字符串。
- `MissionMapper.submitMission` 限制必须本人提交且任务状态为进行中，然后更新为待验收。

## 4. 任务审核结算

入口：

- `POST /api/mission/audit`
- `MissionController.audit`

审核通过链路：

```text
MissionController.audit
-> MissionServiceImpl.auditMission
-> UserMapper.decreaseFrozen
-> TransactionLogMapper.insert
-> UserMapper.increaseBalance
-> TransactionLogMapper.insert
-> MissionMapper.updateStatus
-> RabbitTemplate.convertAndSend
-> ReputationListener.handleMessage
```

核心处理：

- 校验审核人必须是任务发布者。
- 校验任务必须处于待验收状态。
- 扣减发布者冻结余额。
- 增加接单者可用余额。
- 写发布者支出流水和接单者收入流水。
- 更新任务状态为已完成。
- 事务提交后发送 `MissionSettledMsg` 到 RabbitMQ。

审核驳回链路：

```text
MissionController.audit
-> MissionServiceImpl.auditMission
-> MissionMapper.updateStatus
```

当前驳回只将任务状态改回进行中，未发送 MQ。

## 5. 任务取消

入口：

- `POST /api/mission/cancel`
- `MissionController.cancelMission`

链路：

```text
MissionController.cancelMission
-> MissionServiceImpl.cancelMission
-> MissionMapper.selectMissionForUpdate
-> MissionMapper.updateCancelInfo
-> UserMapper.unfreezeBalance
-> TransactionLogMapper.insert
```

核心处理：

- `MissionMapper.selectMissionForUpdate` 使用悲观锁锁定任务。
- 仅允许发布者取消待接单任务。
- 更新任务状态为已取消并记录取消原因。
- 解冻押金，写冻结账户减少和可用账户增加两条流水。

注意：`UserMapper.xml` 中 `unfreezeBalance` 当前更新 `sys_user`，其他用户 SQL 使用 `t_user`，需要确认表名。

## 6. 用户充值

入口：

- `POST /api/user/recharge`
- `UserController.recharge`

链路：

```text
UserController.recharge
-> UserServiceImpl.recharge
-> UserMapper.selectById
-> UserMapper.addBalance
-> TransactionLogMapper.insert
```

核心处理：

- 校验用户存在、状态正常、充值金额合法。
- 原子增加用户余额。
- 写充值资金流水。

## 7. 财务与信誉查询

资金查询入口：

- `GET /api/user/finance/overview`
- `POST /api/user/transaction/list`
- `GET /api/user/finance/charts`

链路：

```text
UserController
-> UserServiceImpl
-> TransactionLogMapper
```

信誉流水查询入口：

- `GET /api/user/reputation/list`

链路：

```text
UserController.getMyReputationLogs
-> ReputationServiceImpl.getMyReputationLog
-> PageHelper.startPage
-> ReputationLogMapper.selectByUserId
```

## 8. 高频改动位置

- 任务状态流转：`MissionServiceImpl`、`MissionMapper.xml`、`MissionStatusEnum`。
- 资金规则：`MissionServiceImpl`、`UserServiceImpl`、`TransactionType`、`UserMapper.xml`、`TransactionLogMapper.xml`。
- 信誉规则：`ReputationListener`、`MissionDifficultyEnum`、`ReputationSourceEnum`。
- 查询筛选：`MissionQueryReq`、`TransactionPageReq`、`MissionMapper.xml`、`TransactionLogMapper.xml`。
- 用户身份：当前多数接口从请求参数传 `userId`，后续接登录态时会影响 Controller 和 Service。

