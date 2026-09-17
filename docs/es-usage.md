# Elasticsearch 使用说明

> 当前项目未发现 Elasticsearch 实际使用。

## 1. 当前状态

未发现以下依赖或代码：

- Elasticsearch Java Client
- Spring Data Elasticsearch
- `ElasticsearchRepository`
- ES 连接配置
- 索引创建或同步逻辑

## 2. 可能的后续接入点

如果后续引入 ES，最可能用于任务搜索：

- 任务标题搜索：`Mission.title`
- 任务描述搜索：`Mission.description`
- 任务类型、难度、最低境界过滤
- 任务状态过滤

当前任务查询由 `MissionMapper.selectList` 通过 MySQL `LIKE` 和条件筛选实现。

## 3. 文档维护要求

新增 ES 后，请同步补充：

- 索引名称和 mapping。
- 数据同步方式：同步写、MQ 异步同步、定时重建。
- 查询入口类和方法。
- MySQL 与 ES 的一致性策略。
- 失败重试和补偿方式。

