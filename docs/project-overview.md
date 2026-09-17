# 项目整体说明

> 本文记录当前对 LWG 项目的整体理解。详细业务链路见 `docs/business-flow.md`，MQ 细节见 `docs/mq-flow.md`，后续改动记录见 `docs/change-log.md`。

## 1. 项目定位

LWG 是一个模拟“宗门任务大厅”的单模块 Spring Boot 项目，主包名为 `com.li.lwg`。当前功能围绕宗门悬赏任务展开：用户发布任务并冻结赏金，其他用户抢单、提交凭证，发布者审核后完成资金结算，并通过 MQ 异步更新接单人的信誉值。

从业务模型看，项目目前包含三个核心域：

- 任务域：悬赏发布、任务查询、抢单、提交、审核、取消。
- 资金域：充值、余额冻结、结算入账、取消退回、资金流水。
- 信誉域：任务结算后根据信誉规则调整用户信誉，并记录信誉流水。

启动类：

- `src/main/java/com/li/lwg/LwgApplication.java`

## 2. 文档索引

| 文档 | 作用 |
| --- | --- |
| `docs/project-overview.md` | 项目整体说明 |
| `docs/business-flow.md` | HTTP 入口到 Service、Mapper、DB 的核心业务链路 |
| `docs/mq-flow.md` | RabbitMQ 生产、路由、消费、幂等链路 |
| `docs/redis-usage.md` | Redis 使用情况和后续接入记录 |
| `docs/es-usage.md` | Elasticsearch 使用情况和后续接入记录 |
| `docs/change-log.md` | 分析、文档、代码改动记录 |
| `docs/codex-guide.md` | Codex 协作开发规则 |
| `docs/cicd-guide.md` | GitHub CI/CD 原理与接入说明 |

## 3. 模块划分

```text
src/main/java/com/li/lwg
├── common        # Result、PageResult 等通用对象
├── config        # 基础设施配置，目前主要是 RabbitMQ
├── controller    # HTTP API 入口
├── dto           # 请求参数、MQ 消息对象
├── entity        # 数据库实体
├── enums         # 业务枚举
├── exception     # 全局异常处理
├── listener      # MQ 消费者
├── mapper        # MyBatis Mapper 接口
├── service       # Service 接口
├── service/impl  # 业务实现与业务编排
└── vo            # 接口返回视图对象
```

资源目录：

- `src/main/resources/application.yml`：与环境无关的公共配置（端口、Jackson、MyBatis、RabbitMQ 凭据、Flyway、Actuator）。
- `src/main/resources/application-dev.yml`：本地开发环境，数据源指向 `localhost:3306/lwg`。
- `src/main/resources/application-prod.yml`：容器生产环境，数据源指向 compose 服务名 `lwg-mysql` / `lwg-rabbitmq`。
- `src/main/resources/db/migration/V*.sql`：Flyway 迁移脚本，表结构的唯一事实来源（`V1__init_schema.sql` 为初始四张表）。
- `src/main/resources/mapper/*.xml`：MyBatis SQL 映射。
- 根目录 `.env.example`：环境变量模板；真实 `.env` 已被 `.gitignore` 忽略。

测试目录：

- `src/test/java/com/li/lwg/AbstractIntegrationTest.java`：集成测试基类，静态启动一次性 MySQL 与 RabbitMQ 容器并用 `@DynamicPropertySource` 注入连接信息，保证测试不连开发者的本机库。
- `src/test/java/com/li/lwg/LwgApplicationTests.java`：上下文冒烟 + 数据源隔离性断言 + Flyway V1 迁移回归。
- `src/test/java/com/li/lwg/service/UserRechargeTest.java`：充值正确性与并发原子性（100 线程），含余额精确性、非法金额拒绝、流水条数一致性断言。
- `src/test/resources/fixtures/*.sql`：测试夹具，固定主键、幂等（先删后插）。

本地执行测试的注意事项（国内网络下 Docker Hub 不可达时）：

```bash
TESTCONTAINERS_RYUK_DISABLED=true mvn test \
  -Dlwg.test.mysql.image=arm64v8/mysql:latest \
  -Dlwg.test.rabbitmq.image=arm64v8/rabbitmq:3.13.7
```

## 4. 技术栈与基础设施

| 组件 | 用途 |
| --- | --- |
| Spring Boot Web | 提供 REST API |
| MyBatis | Mapper 接口 + XML 访问 MySQL |
| MySQL | 存储用户、任务、资金流水、信誉流水 |
| RabbitMQ | 任务结算后异步通知信誉系统 |
| Testcontainers | 测试期临时容器，保证 `mvn test` 不污染开发库且 CI 可独立运行 |
| Spring Transaction | 保证任务、资金、流水更新的事务一致性 |
| PageHelper | 信誉流水分页 |
| Jackson ObjectMapper | 任务提交凭证 JSON 序列化 |
| Flyway | 数据库版本管理，启动时自动执行 `db/migration` 下的迁移 |
| Spring Actuator | `/actuator/health` 探活，供容器 HEALTHCHECK 与部署脚本使用 |
| Spring Profile | dev / prod 环境隔离，生产凭据全部来自环境变量 |

当前未发现实际使用：

- Redis
- Elasticsearch
- Dubbo / RPC
- Feign
- Kafka / RocketMQ
- 定时任务
- 配置中心

## 5. 常见入口类型

### HTTP Controller

- `MissionController`：任务发布、任务查询、抢单、提交、审核、取消。
- `UserController`：用户信息、充值、资金流水、财务图表、信誉流水。

### MQ Listener

- `ReputationListener.handleMessage`：监听 `lwg.reputation.queue`，消费 `MissionSettledMsg`，更新用户信誉并写入信誉流水。

### 暂未使用的入口

- Job / Scheduler：未发现。虽然 `Mission.deadline` 已存在，但当前没有超时任务处理。
- RPC / Dubbo：未发现。

## 6. 核心能力概览

### 任务能力

- `MissionServiceImpl.publishMission`：发布任务，冻结发布者余额，写资金流水。
- `MissionServiceImpl.getMissionList`：按条件查询任务，当前默认查询待接单任务。
- `MissionServiceImpl.acceptMission`：抢单，校验任务状态、接单人身份、境界要求，并使用 `version` 乐观锁。
- `MissionServiceImpl.submitMission`：提交任务凭证，将 `proofData` 序列化为 JSON。
- `MissionServiceImpl.auditMission`：审核任务，审核通过后完成资金结算并发送 MQ。
- `MissionServiceImpl.cancelMission`：取消待接单任务，悲观锁锁定任务并解冻押金。

### 资金能力

- `UserServiceImpl.recharge`：用户充值，更新余额并写 `t_transaction_log`。
- `UserServiceImpl.getFinanceOverview`：查询累计收支和本月收支。
- `UserServiceImpl.getTransactionPage`：按时间、分类筛选资金流水。
- `UserServiceImpl.getFinanceCharts`：查询近 12 个月收支趋势和类型分布。

### 信誉能力

- `ReputationListener.handleMessage`：消费任务结算消息，按任务难度调整信誉值。
- `ReputationServiceImpl.getMyReputationLog`：分页查询用户信誉流水。

## 7. 数据与消息流

主要表：

- `t_user`：用户、余额、冻结余额、境界、信誉。
- `t_mission`：任务主体、发布者、接单者、状态、凭证、截止时间。
- `t_transaction_log`：资金流水。
- `t_reputation_log`：信誉流水。

主要消息链路：

```text
MissionServiceImpl.auditMission
-> 事务提交后 MissionServiceImpl.sendMessage
-> RabbitTemplate.convertAndSend
-> lwg.mission.exchange / mission.settled
-> lwg.reputation.queue
-> ReputationListener.handleMessage
```

消息体：

- `MissionSettledMsg`

## 8. 关键类清单

| 类 / 文件 | 作用 |
| --- | --- |
| `MissionController` | 任务 HTTP API 入口 |
| `UserController` | 用户、资金、信誉 HTTP API 入口 |
| `MissionServiceImpl` | 任务主流程编排，当前项目最核心类 |
| `UserServiceImpl` | 用户充值、财务统计、资金流水查询 |
| `ReputationServiceImpl` | 信誉流水分页查询 |
| `ReputationListener` | 任务结算后的信誉异步处理 |
| `RabbitConfig` | RabbitMQ exchange、queue、binding 配置 |
| `MissionMapper.xml` | 任务状态流转 SQL |
| `UserMapper.xml` | 用户余额、冻结余额、信誉 SQL |
| `TransactionLogMapper.xml` | 资金流水与财务统计 SQL |
| `ReputationLogMapper.xml` | 信誉流水 SQL |
| `GlobalExceptionHandler` | 统一异常返回 |

## 9. 优先理解的 10 个类

1. `MissionServiceImpl`：任务、资金、MQ 的核心编排类。
2. `MissionController`：任务 API 从这里进入。
3. `MissionMapper`：任务持久化接口。
4. `MissionMapper.xml`：任务查询、乐观锁、悲观锁 SQL。
5. `UserServiceImpl`：充值、资金统计、流水查询。
6. `UserMapper`：用户余额、冻结余额、信誉更新接口。
7. `UserMapper.xml`：用户资金 SQL，需要重点确认表名。
8. `TransactionLogMapper.xml`：资金流水和财务报表的数据来源。
9. `ReputationListener`：任务结算后信誉处理入口。
10. `RabbitConfig`：任务结算消息如何路由到信誉系统。

## 10. 当前高风险或待确认点

- `UserMapper.xml` 中 `unfreezeBalance` 使用 `sys_user`，其他用户相关 SQL 使用 `t_user`，需要确认真实表名。
- `Mission.deadline` 已存在，但当前没有定时任务处理任务超时。
- `MissionAuditReq.remark` 当前未在审核驳回链路中落库。
- `application.yml` 开启 RabbitMQ publisher confirm/return，但当前未看到确认回调处理。
- `ReputationListener` 支持 `pass=false` 的扣分逻辑，但 `MissionServiceImpl.auditMission` 驳回分支当前不发送 MQ。
- 当前用户身份通过请求参数传入，如 `publisherId`、`acceptorId`、`userId`，还没有登录态或权限上下文。
- 没有看到数据库 DDL，索引、唯一约束、字段默认值需要补充确认。

## 11. 后续演进方向

基础方向：

- 任务分页和排序：改造 `MissionQueryReq`、`MissionServiceImpl.getMissionList`、`MissionMapper.xml`。
- 任务详情接口：补充任务、发布者、接单者、状态描述。
- 任务超时机制：新增定时任务，处理待接单超时和进行中超时。
- 审核驳回原因：复用 `MissionAuditReq.remark`，补充落库字段或审核记录表。

有趣玩法：

- 宗门贡献值：完成任务增加贡献，可兑换称号或特殊任务资格。
- 境界突破：结合 `User.realm`、信誉、贡献、完成任务数设计突破规则。
- 任务评价：审核通过后发布者给接单者打分和评价。
- 连环任务：通过 `parent_id` 或 `chain_id` 设计多阶段任务。
- 稀有任务刷新：定时生成系统任务，如宗门急令、秘境任务、长老委托。

技术练习：

- Redis 幂等或限流：用于抢单、审核、充值等入口。
- RabbitMQ 死信队列：完善消费失败后的补偿链路。
- Elasticsearch 任务搜索：替代当前 `MissionMapper.selectList` 中的 MySQL `LIKE`。
- 操作日志/审计日志：记录发布、抢单、提交、审核、取消等关键动作。

