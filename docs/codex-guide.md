# Codex 协作指南

> Codex 是本项目的协作开发者。每次开始分析或修改前，应先阅读本指南和项目核心文档。

## 1. 启动必读

每次 Codex 启动或开始新任务时，先阅读：

1. `docs/project-overview.md`
2. `docs/business-flow.md`

如任务涉及已有改动、上下文延续或代码修改，再阅读：

3. `docs/change-log.md`

如果任务涉及 MQ、Redis、ES，还需要阅读：

- `docs/mq-flow.md`
- `docs/redis-usage.md`
- `docs/es-usage.md`

## 2. 工作原则

- 先分析再修改。
- 最小改动原则。
- 不破坏现有逻辑。
- 保持代码风格一致。
- 不明确时先询问。
- 输出尽量结合实际类名、方法名、包名和配置文件，不泛泛而谈。

## 3. 修改代码后的要求

每次完成代码修改后，必须同步更新 `docs/change-log.md`，记录：

- 改动目的。
- 涉及类、方法、Mapper 或配置文件。
- 是否执行测试或未执行测试的原因。

如果改动影响以下内容，也要同步更新对应文档：

- 业务链路变化：更新 `docs/business-flow.md`。
- MQ 生产、消费、路由、重试、幂等变化：更新 `docs/mq-flow.md`。
- Redis 使用变化：更新 `docs/redis-usage.md`。
- Elasticsearch 使用变化：更新 `docs/es-usage.md`。
- 项目结构、关键类、入口变化：更新 `docs/project-overview.md`。

## 4. 输出要求

- 分析说明要引用实际类名和方法名，例如 `MissionServiceImpl.auditMission`。
- 涉及 SQL 时引用 Mapper 和 XML，例如 `UserMapper.unfreezeBalance`、`UserMapper.xml`。
- 涉及接口时引用 Controller 方法和路径，例如 `MissionController.publish`、`POST /api/mission/publish`。
- 说明改动时优先使用“入口 -> 编排 -> 核心处理 -> 持久化/消息/下游”的结构。
- 如果无法确认事实，明确标注“不确定”并说明需要用户补充什么背景。
