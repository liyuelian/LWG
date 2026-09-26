# 天道碑排行榜

## 玩法口径

- 信誉榜：正常账号按当前信誉降序，NULL 按初始值 6000。
- 悬赏完成榜：正常账号作为接单者且 `t_mission.status = 3` 的已验收任务数；至少完成一单才上榜。
- 同分并列，采用竞赛排名，如 1、1、3；同分按用户 ID 升序展示。
- 默认前 50 位、上限 100 位；个人名次按全榜计算，不受截断影响。
- 未提供用户、用户不存在/禁用或未达到上榜条件时，`myRank = null`。
- 当前只支持总榜；`finish_time` 未写入，暂不支持月榜。

## 技术链路

```text
GET /api/rank/board
-> RankController.getBoard
-> RankServiceImpl.getBoard
-> RankIndex (Redis ZSET)
-> RankMapper.selectProfiles (MySQL 批量取最多 100 个道号、境界)
```

首次启动与每日校准使用 `RankIndex.rebuild` 分批从 MySQL 导入分数，普通查询从 Redis 获取前 N 名、人数和个人分数。悬赏结算后 `MissionServiceImpl.auditMission` 更新完成榜；MQ 消费并提交信誉变动后 `ReputationListener.handleMessage` 更新信誉榜。更新使用 MySQL 最终值以避免重复累加。Redis 键、同步时机、故障恢复与多实例限制见 [redis-usage.md](redis-usage.md)。

前端 `lwg-ui/src/components/RankScroll.vue` 是任务大厅和个人中心共用的卷轴弹窗；旧 `/rank` 链接重定向到任务大厅并自动打开弹窗。包括榜首卡片、本人高亮、个人名次、规则说明、刷新、空榜、就地错误重试和手机布局。信誉在 Redis/MySQL 中仍为整数原值（如 6000），界面统一除以 100 显示两位小数（60.00 分），与个人中心一致；悬赏完成数直接显示整数。

## HTTP 契约

`GET /api/rank/board?type=reputation&limit=50&userId=1`

| 参数 | 默认值 | 规则 |
| --- | --- | --- |
| type | reputation | reputation / completed |
| limit | 50 | 整数，1–100 |
| userId | 不传 | 可选正整数，用于个人排名 |

响应沿用 `Result` 包装；参数错误使用已有业务 `code = 400`、HTTP 200 约定。

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "type": "completed",
    "total": 3,
    "entries": [
      { "rank": 1, "userId": 2, "username": "青云剑仙", "realm": 5, "score": 18 }
    ],
    "myRank": { "rank": 3, "userId": 1, "username": "山间明月", "realm": 2, "score": 9 }
  }
}
```

上例为 `limit=1` 的示意数据。`V2__ranking_index.sql` 为完成榜首次建榜及单用户刷新增加 `(status, acceptor_id)` 索引。

## 验证

`RankBoardTest` 使用 Testcontainers + MockMvc 覆盖真实 MySQL 与 Redis 行为。测试容器不会写本地开发库。国内网络无法访问默认镜像时可用本地缓存镜像：

```bash
TESTCONTAINERS_RYUK_DISABLED=true mvn test \
  -DargLine=-Dfile.encoding=UTF-8 \
  -Dlwg.test.mysql.image=arm64v8/mysql:latest \
  -Dlwg.test.rabbitmq.image=arm64v8/rabbitmq:3.13.7 \
  -Dlwg.test.redis.image=arm64v8/redis:latest
```

浏览器页面状态的测试使用明确的模拟接口响应；真正的榜单数据读取由后端集成测试及本地运行接口验证。
