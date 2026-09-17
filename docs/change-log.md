# 变更记录

> 每次完成分析、文档更新或代码修改后，请追加记录。记录应说明改动目的、涉及文件、是否执行验证。

## 2026-09-19

### 代码（前端仓库 lwg-ui）：按 `frontend-redesign.md` 完成视觉与交互改版

改动目的：落地 `docs/frontend-redesign.md` 的规格——**宣纸水墨打底 + 天道碑局部暗色**，并修掉该文档第 1 节列出的三个基础问题（外链字体不可达、Element 默认蓝与主色打架、`index.html` 三件小事）。

涉及文件（均在 **lwg-ui 仓库**，本仓库只更新文档）：

| 文件 | 改动 |
| --- | --- |
| `index.html` | `lang="zh-CN"`、`<title>灵务阁</title>`、favicon 换为印章风格 `public/seal.svg`（朱砂方印 + 白文「灵」）、补 `theme-color`/`description` |
| `public/seal.svg` | **新增**，手写 SVG，无外部依赖 |
| `src/style.css` | 重写为全量设计 token：色彩 / 暗色 `.lwg-night` 作用域 / 字体栈 / 字号阶梯 / 圆角 / 间距 / `--el-*` 覆盖 + 全局滚动条、选中色、路由过渡、Element 细节对齐，并抽出一层原子类（`lwg-btn` / `lwg-card` / `lwg-tabs` / `lwg-status` / `lwg-inline-error` / `lwg-empty` / `lwg-num`） |
| `src/main.js` | **修正样式加载顺序**（见下）＋ Element Plus 中文 locale |
| `src/App.vue` | `router-view` 加 120ms fade 过渡，移除硬编码 body 背景 |
| `src/components/AppLayout.vue` | **新增**，共用顶栏（灵务阁标题 + 任务大厅/个人中心/天道碑/退出）、路由高亮、退出确认、`< 960px` 折叠为汉堡菜单、墨线页脚 |
| `src/views/Login.vue` | 移除 Google Fonts；改用 token；**改写「凡人测试模式：请输入 1 或 2」**为「凭弟子令牌入阁…」；改为 `<form>` 提交 + 就地校验（正整数、空值），不再用 toast 报错 |
| `src/views/MissionHall.vue` | 套 `AppLayout`；移除 `@import`；表格 + **卡片流双形态**（`< 960px` 切换）；前端分页；接榜/撤榜改为 `inlineError` **就地反馈**＋原位变更状态（不整页刷新）＋按钮 loading；`getStatusText` 与后端 `MissionStatusEnum` 对齐（`1 → 进行中`、`2 → 待验收`）；删除硬编码表头底色 |
| `src/views/UserDashboard.vue` | 套 `AppLayout`；移除 `@import`；**`window.addEventListener('resize', …)` 改为 `ResizeObserver` + `onUnmounted` 统一释放**；ECharts 配色改从 token 运行时读取；抽出 `FrozenFundCard`；文案「冻结/退款」→「内部流转」；子表格补窄屏卡片形态 |
| `src/components/FrozenFundCard.vue` | **新增**，`/user/frozen/list` 冻结中明细 + 可选对账面板（`?dev=1` 开启），接口不可用时降级为引导态而非 toast |
| `src/views/RankBoard.vue` | **新增**，整页 `.lwg-night` 暗色；信誉榜/接单王榜 Tab + 月榜置灰 tooltip；Top3 金银铜徽记与微光；当前用户行高亮；吸底「我的名次」卡片；`ranked:false` 走引导文案 |
| `src/api/rank.js` | **新增**，`getRankBoard` / `getMyRank` |
| `src/api/mission.js` | 迁入 `getMyMissions`（原在 `user.js`，但打的是 `/mission/my-missions`）；事务类接口放宽超时；`acceptMission` / `cancelMission` 支持 `inlineError` |
| `src/api/user.js` | 移除 `getMyMissions`；新增 `getFrozenList` / `getFrozenHistory` / `getReconcile`；充值放宽超时 |
| `src/utils/request.js` | 事务类 `TX_TIMEOUT = 15000`；新增 `config.inlineError` 开关（为 true 时不弹 toast，交调用方就地渲染）与 `errText()`；HTTP 状态码映射为宗门文案 |
| `src/utils/theme.js` | **新增**，token 运行时读取（`cssVar` / `token`）＋兜底字面量集中处 |
| `vite.config.js` | `preview` 也配 `/api` 代理（否则无法验证 `dist` 真实产物）；新增构建插件剔除 `@fontsource` 的 `.woff` 回退 |
| `src/assets/vue.svg`、`src/components/HelloWorld.vue` | 删除脚手架残留 |

四个值得记下来的坑：

1. **`style.css` 必须排在 `element-plus/dist/index.css` 之后**。两者对 `:root` 的 `--el-*` 变量同优先级（都是 `:root`），靠源码顺序决胜；原来的顺序会让 Element 的默认蓝 `#409eff` 盖掉朱砂覆盖——表现为弹窗按钮、分页仍是蓝色。这是 AC3 第一条失败的原因。
2. **本项目全局 reset 不含 `border-box`**，`AppLayout` 页脚 `width:100%` + `padding` 在窄屏撑出 48px / 24px 横向滚动条（AC5 失败原因），已显式声明并全量复查了同类写法。
3. **`@fontsource` 的 CSS 是被 `@import` 内联进同一个模块的**，`transform` 钩子拿不到它，剔除 woff 回退必须挂在 `generateBundle`；且压缩后引号会统一成双引号（只匹配单引号会静默失效）。
4. **Element Plus 默认英文**：不设 locale 分页会显示 `Total 1`，已改为 `zh-cn`。

字体决策（原文档待确认问题 2）：采用 `@fontsource/noto-serif-sc`，按 unicode-range 切片、`font-display: swap`，**只用于标题**；正文仍用系统 sans 栈。构建产物含 196 个 woff2 分片，但浏览器按 unicode-range 只取实际用到的分片（实测每个页面 4–12 个请求），已剔除 `.woff` 回退使 `dist` 从 17 MB 降到 9.4 MB。样式表因此从 350 kB 增至 575 kB（纯 `@font-face` 声明）。

**验证（CDP 驱动真实浏览器，非静态检查）**：Chrome 153 headless + Node 内置 WebSocket，无第三方依赖。对 `dist` 真实产物（`vite preview` + `/api` 反向代理到后端）跑完 32 条断言，全绿，全程 0 条 JS 报错：

- **AC1** Google Fonts 请求 0 条；本地 woff2 12 个请求；`document.fonts.check('700 28px "Noto Serif SC"')` 为真；标题 computed 为 `Noto Serif SC 700 28px`、字距 3px（原 10px）
- **AC2** `grep "#8b3a3a" src/views src/components` 无结果；色值只存在于 `style.css`（token 定义）与 `utils/theme.js`（ECharts 兜底）两处；正文底色 `rgb(245,240,230)`
- **AC3** `--el-color-primary = #8b3a3a`；`ElMessageBox` 确认键底色 `rgb(139,58,58)`；`el-steps` 完成节点 `rgb(139,58,58)`；分页当前页朱砂底
- **AC4** `title="灵务阁"`、`lang="zh-CN"`、favicon `/seal.svg`
- **AC5** 1440 / 768 / 375 三档横向溢出均为 0px；768px 起表格切卡片流、汉堡菜单出现且可展开导航；375px 天道碑同样 0px
- **AC6** 天道碑 → 任务大厅切页在 420ms 内采样 27 帧、空白帧 0；全程 `console.error` / 未捕获异常 0 条
- **AC7** `/rank` 内 `--lwg-paper=#12161c`、`--lwg-gold=#d4af6a`；**body 背景始终是宣纸色**；返回大厅后 `.lwg-night` 无残留、根 `--lwg-paper` 回到 `#f5f0e6`
- **AC8** 用道友 #2（境界 5）接 `minRealm=9` 的悬赏，真实后端拒绝：卡片内就地显示「道友修为尚浅，此悬赏需【渡劫期】方可接取！」、**toast 为 0 条**、约 2s 自动淡出、**任务数据未被改动**（status 仍 0、acceptor 仍 null）；另注入业务码非 200 响应（同时覆盖 axios 的 fetch 与 XHR 两条适配链路）验证同一分支
- **AC9** 从 `/dashboard` 切走后旧图表 canvas 残留 0，连续 5 次窗口尺寸变更新增报错 0
- **AC10** 完整闭环走真实界面（点击驱动，非直接调接口）：道友 #2 充值 300（25000 → 25300）→ 道友 #1 发布悬赏 #5（冻结 10000 → 10120、可用 −120）→ 道友 #2 接榜（列表状态原位「待接单」→「进行中」，未整页刷新，行高亮 + 同步提示）→ 提交复命（status 2，`proofData` 落库）→ 道友 #1 批复通过（弹窗确认键朱砂）→ status 3、押金扣划回 10000、道友 #2 可用 25300 → 25420。全程 0 报错
- 另外跑了 `npm run build`：构建通过，无编译错误

**测试数据已还原**：AC10 产生的任务 #5/#6、充值流水 id 17–26 已删除，`t_user.balance` 还原为 `85000 / 25000`。还原后复核对账等式：R1/R2 两人均 `balance = SUM(流水 asset_type=1)`、`frozen_balance = SUM(asset_type=2)` 为 OK；R3 `SUM(frozen_balance) = SUM(reward WHERE status IN (0,1,2))` = 10000 = 10000 OK。

**与后端需求的衔接（现状说明，非缺陷）**：`/api/rank/*`、`/api/user/frozen/*`、`/api/admin/reconcile` 目前后端尚未实现（网关返回 404，`Result` 包装后表现为 `code:500`）。前端已按 `requirement-ranking.md` 第 4 节与 `requirement-frozen-detail.md` 第 5 节的契约写好调用，并在这些接口不可用时**降级为引导态**（天道碑显示"碑文需待后端榜单接口开放后方可刻录"、冻结区块显示"冻结明细接口暂不可用"＋重试），不报错、不弹 toast。后端落地后无需改前端。月榜 Tab 已按契约**置灰并给 tooltip**（依赖 D1 `finish_time` 落库），不会因 `period=month` 的 400 而报错。

## 2026-09-18

### 文档：新增 lwg-ui 视觉与交互改版规格（已确认方向）

改动目的：用户选定前端「视觉与交互改版」，且确定**在 `lwg-ui` 目录另开会话实施**，而该会话看不到本次对话上下文，需要一份可独立交接的规格文档。

已确认的设计方向：**宣纸水墨打底 + 天道碑（排行榜）局部暗色**——由用户在"只做宣纸 / 全站暗色星象 / 局部暗色"三个方向中选定。

涉及文件：

| 文件 | 改动 |
| --- | --- |
| `docs/frontend-redesign.md` | 新增。含设计 token（色彩 / 暗色局部作用域 / 字体 / 形状间距）、Element Plus 变量覆盖、结构改造、交互改造、逐文件改动清单、天道碑页面视觉规格、10 条验收清单、5 个待确认问题，以及可直接复制的"新会话启动提示" |
| `docs/project-overview.md` | 更新文档索引，补充该条目 |

本次为确定方向而对 `lwg-ui` 做的**只读**排查结果（`/Users/liyuelian/Codes/IdeaCode/lwg-ui`，未修改任何文件）：

1. **`index.html` 仍是脚手架默认值**：`lang="en"`、`<title>lwg-ui</title>`、favicon 为 `/vite.svg`。
2. **Element Plus 主题零定制**：`main.js` 直接引 `element-plus/dist/index.css`，`style.css` 的 `:root` 只设了 `font-family`，全项目 `--el-color-primary` 无覆盖 → `ElMessageBox` / `el-pagination` / `el-steps` 显示 Element 默认蓝 `#409eff`，与页面朱砂色 `#8b3a3a` 同屏冲突。
3. **Google Fonts 外链不可用**：`Login.vue:61`、`MissionHall.vue:440`、`UserDashboard.vue:577` 三处各自 `@import` Noto Serif SC。生产部署在阿里云、用户在国内，该域名不可达，标题实际 fallback 为系统衬线字体，且 `@import` 阻塞渲染。
4. **零响应式**：`grep -rn "@media" src/` 匹配 **0 处**。
5. **主色硬编码 25 处**，分布于 3 个视图文件（`grep -rno "#8b3a3a"` 计数）。
6. **`UserDashboard.vue:519` 的 resize 监听未解绑**：`window.addEventListener('resize', ...)` 使用匿名函数且全文件无 `onUnmounted` / `onBeforeUnmount`，路由切走后监听仍持有旧 ECharts 实例。
7. **`MissionHall.vue` 状态文案自相矛盾**：第 325 行 `getStatusText` 为 `1 → 修仙中`、`2 → 待结算`，与后端 `MissionStatusEnum`（`进行中` / `待验收`）不符，而同一文件第 26 行的筛选按钮已使用「进行中」。
8. `Login.vue` 的"登录"仅把 UID 写入 `localStorage.lwg_user_id`（默认值 `1`），`router.beforeEach` 只判断该值是否存在，页面文案为"凡人测试模式：请输入 1 或 2"。

验证：

- 本次为纯文档改动 + 对 `lwg-ui` 的只读排查，**未修改 `lwg-ui` 任何文件**（该仓库在本会话工作区之外），未运行前端构建，未运行 `mvn test`。
- 上述 8 条均通过 `grep` / `cat` 实测确认（第 1、2、3、4、5、6、7、8 条为对文件内容的直接观察）。

未执行：未改动 `lwg-ui` 的样式与组件，未实施 `docs/frontend-redesign.md` 中的任何改动。

## 2026-09-18

### 文档：新增「天道碑排行榜」与「冻结资金明细化」两份需求

改动目的：用户选定 `MissionServiceImpl` 之外的两个新功能方向（排行榜、冻结资金明细），需要先固化需求与验收标准，避免直接进入编码。

涉及文件：

| 文件 | 改动 |
| --- | --- |
| `docs/requirement-ranking.md` | 新增。定义信誉榜与接单王榜的口径、接口、VO、V2 索引迁移、性能策略、11 条验收标准、前端改动点、6 个待确认问题 |
| `docs/requirement-frozen-detail.md` | 新增。梳理冻结/释放的全部 3 条 SQL 路径，对比「派生视图 / 明细表 / 事件溯源」三种方案，给出 P0（派生视图 + 对账断言 R1–R5）与 P1（`t_frozen_detail` + 回填 + 双写约束）需求、9 条验收标准、6 个待确认问题 |
| `docs/project-overview.md` | 更新文档索引，补充上述两份文档条目 |

本次分析新发现的代码问题（已写入需求文档，尚未修复）：

1. **`t_mission.finish_time` 从不写入**：全项目只有 `MissionMapper.xml` 的 resultMap / `Base_Column_List` 与 `Mission.finishTime` 的 getter/setter 提到它，**没有任何 UPDATE 写这一列**，永远是 NULL。它阻断「接单王月榜」（`WHERE finish_time >= ?` 永远匹配 0 行），因此排行榜需求把 D1 列为前置依赖。
2. **`MissionMapper.updateStatus` 缺状态守卫**：`updateCancelInfo` 已在 2026-09-17 补上 `AND status = 0`，但 `updateStatus` 没有，而 `MissionServiceImpl.auditMission` 直接调用它。两个并发审核请求都能通过 `status != 2` 检查，只要发布者还有别的任务冻结着钱，`UserMapper.decreaseFrozen` 就会用**其他任务的押金**完成第二次结算。建议与 D1 在同一次改动里改为带守卫的 `finishMission(id, fromStatus, finishTime)`。
3. **`UserMapper.updateReputation` 是绝对赋值**（`SET reputation = #{reputation}`）配合 `selectById` 读改写，并发消费两条 MQ 消息会丢更新；且 `t_reputation_log` 上没有 `(source_type, source_id)` 唯一索引，`ReputationLogMapper.countBySource` 的幂等只是应用层检查。
4. **`POST /api/user/recharge` 无鉴权、无签名、无次数限制**：请求体传 `userId` + `amount`（单次上限 1 亿）即可为任意账户无限充值，是全项目唯一资金来源，也是当前最严重的敞口。
5. **`t_user.status` 只在 `UserServiceImpl.recharge` 与 `UserMapper.addBalance` 的 `status = 1` 处被校验**，被封印账号仍可发布、抢单、提交、审核、撤榜。
6. **`MissionAuditReq.pass` 是 `Boolean` 且无 `@NotNull`**，`MissionServiceImpl.auditMission` 中 `if (req.getPass())` 拆箱会 NPE→500；`MissionAcceptReq`、`MissionSubmitReq`、`MissionAuditReq` 三个 DTO 均无校验注解。
7. **`MissionMapper.selectMyMissions` 的 `type` 传 1/2 之外的值会返回全表**（两个 `<if>` 都不命中，只剩 `WHERE 1=1`），目前仅靠 `MissionController.getMyMissions` 的参数校验挡住。
8. **`/api/user/transaction/list` 的 `category=locked` 名不副实**：`UserServiceImpl.getTransactionPage` 用的是 `TransactionType.getInternalTypes()`（`PUBLISH` 与 `REFUND`），返回的是发布/退回流水，**不是"当前冻结余额"**，前端文案需要区分。

验证：

- 本次为纯文档改动，未修改任何 Java、XML、SQL 或前端代码，未运行 `mvn test`，未连接数据库。
- 上述代码问题均为**阅读源码得出**，其中第 1、2、7 条用 `grep` 确认了"零写入点 / 零守卫 / 条件不命中"的事实；**尚未在运行环境实测复现**，编码前需按 `docs/codex-guide.md` 先验证再动手。

未执行：未编写 `V2__*.sql` 迁移（排行榜索引与冻结明细索引仍在需求阶段），未改动 `lwg-ui`。

## 2026-09-17

### 部署：阶段 5 完成阿里云生产部署

改动目的：把应用真正部署到阿里云服务器，验证 CI 产物可运行、并建立可回滚的部署流程。

服务器侧准备（均为实测）：

- 配置 `/etc/docker/daemon.json` 镜像加速器（服务器直连 Docker Hub 超时；`ghcr.io` 可直连）；
- 新增 `/swapfile2`（2G，写入 fstab）并把 `vm.swappiness` 设为 10，**总 swap 6G**；
- 创建 `lwg` bridge 网络与 `/opt/lwg` 部署目录；
- 生成 `/opt/lwg/.env`（随机 24 位密码，权限 600，不入库）；
- 用 `docker run` 创建两个数据容器（**刻意不交给 compose 管理，保护数据卷**）：
  `lwg-mysql`（MySQL 8.4.11，320M 限额，数据落 `/opt/lwg/data/mysql`）、
  `lwg-rabbitmq`（3.13，192M 限额，未启用管理插件）。

仓库新增文件：

| 文件 | 作用 |
| --- | --- |
| `deploy.sh` | 部署脚本：校验环境 → 记录回滚点 → 拉镜像（失败重试 3 次）→ 起容器 → 轮询健康检查 → 失败自动回滚 |
| `docs/deployment.md` | 服务器构成、资源限额、部署流程、已知坑与排查方法 |
| `.env.example` | 补充 `BACKEND_IMAGE` / `FRONTEND_IMAGE` 两个变量 |

验证（均为实测）：

- `deploy.sh` 执行**退出码 0**；四个容器全部 healthy，`RestartCount=0`、`OOMKilled=false`、dmesg 无 OOM 记录；
- **Flyway 在全新生产库正确执行迁移**：`<< Empty Schema >>` → `Migrating to version "1 - init schema"` → `now at version v1`，四张业务表建出；
- 后端以 **prod profile** 连接 `lwg-mysql`（MySQL **8.4**，与本地 9.2 不同版本，迁移脚本两边均通过），启动耗时 8.3s；
- 经 nginx 走完整链路（nginx → backend → mysql）验证 `/api/user/info`、`/api/mission/list`、财务与信誉接口均返回 200；
- 站点端口探活 `http://127.0.0.1:8081/actuator/health` → 200；
- 内存占用实测：backend 221M / mysql 214M / rabbitmq 115M / frontend 6M，合计约 556M，服务器可用 482M。

过程中发现并修复的问题：

1. **拉取 ghcr.io 镜像卡死**：首次部署卡了 10 分钟。排查发现 CDN 把某个层（30.9MB）调度到极慢节点——实测同一层出现 `18KB/s` 与 `11MB/s` 两种速度，节点从 `ghcrblobs18` 换到 `ghcrblobs10` 即恢复。据此给 `deploy.sh` 加了拉取重试（`PULL_RETRIES`，递增退避）。
2. **`.env` 缺镜像变量**：首次部署是手动传入 `BACKEND_IMAGE`/`FRONTEND_IMAGE`，无人值守时会失败。已补入服务器 `.env` 与仓库模板，并**重跑一次不带环境变量的 `deploy.sh` 验证退出码 0**。

数据状态：生产库 `t_user` 有 2 条记录（为端到端验证而建，id=1 发布者_老祖 / id=2 接单者_张三），其余三张业务表为空。

### 修正：为初始资金补记流水（我重复犯了同一错误）

创建测试用户时直接 `INSERT` 了 `balance`（100000 / 20000）却**未写对应的资金流水**，
导致财务对账页 `totalIncome=0` 而余额 10 万，无法解释来源；对账检查直接报「不平」
（余额 100000 vs 可用钱包流水净额 0）。

已补记两条 `type=5`（灵石充值）流水，`balance_after` 分别为 100000 / 20000。
修复后：两个用户对账均为「平」，`frozen_balance` 合计与待结算赏金合计均为 0，
财务概览接口返回 `totalIncome: 100000`。

**这是本项目第二次犯同一个错误**——开发阶段曾用近似对账等式清理流水导致误删
（见本文档前文的「事故记录」）。两次的根因相同：**以 SQL 直接改动余额时忽略了流水**。
已把「注入余额必须同时写充值流水」写入 `docs/deployment.md` 作为固化规范。

未完成（需人工操作）：

- **安全组放行 8081/TCP** —— 当前公网访问返回 `HTTP 000`，服务器侧已监听 `0.0.0.0:8081`；
- **自托管 Runner 注册** —— 需要 GitHub token，沙箱无法读取 macOS Keychain（实测报 `-67674`）。

### 数据：清空开发库业务数据（仅保留 t_user）

改动目的：在一次失误的流水清理之后（经过见下），为得到可验证的一致基线，按用户要求清空业务数据、只保留用户表。

执行内容（均为实测）：

```sql
TRUNCATE TABLE lwg.t_transaction_log;
TRUNCATE TABLE lwg.t_reputation_log;
TRUNCATE TABLE lwg.t_mission;
UPDATE lwg.t_user SET balance=0, frozen_balance=0, version=0, reputation=6000;
```

- 清空前三张表后 **`flyway_schema_history` 保持不动**，因为 schema 本身未回退，回滚历史会让 Flyway 在下次启动时重复执行 V1。
- 用户的两条记录（`发布者_老祖` / `接单者_张三`）保留，资金与信誉重置为初始值。

清空后回归验证：

- 空库下 8 个接口全部正常：`/actuator/health`、`/api/user/info`、`/api/mission/list`、`/api/mission/my-missions`、`/api/user/finance/overview`、`/api/user/finance/charts`（聚合与补零逻辑未因空表报错）、`/api/user/transaction/list`、`/api/user/reputation/list`；
- 随后跑通完整业务闭环：充值 → 发布 → 抢单 → 提交 → 审核通过，以及发布 → 撤榜；
- **对账全部为平**：两个用户的 `balance` 均等于其 `asset_type=1` 流水净额（95000=95000、25000=25000），`t_mission` 待结算赏金合计与 `t_user.frozen_balance` 合计均为 0；10 条流水分属 6 个单号，撤榜的两条共用 `f90b0619`。

### 事故记录：误删开发库历史流水（重要）

**经过**：阶段 1 提交前用旧版测试（`UserRechargeTest.testConcurrentRecharge`，连开发库真实充值 1000 次）验证，产生约 1000 条充值流水。随后我用 `DELETE FROM lwg.t_transaction_log WHERE id >= 1167` 清理，**但未事先备份**，误删了用户原有的历史流水（含全部 `mission_id` 非空的业务流水）。

**错误放大**：为"精确还原缺失条数"，我构造了一个对账等式 `余额 − 初始充值 = 可用余额流水净额` 并据此计算差额。该等式不成立——开发库中存在手工注入、**没有对应流水**的初始资金（如 id 47 的 9999），因此算出的"缺 -153 条"完全错误，导致第二次误删。

**教训**：

1. 对开发库执行任何 `DELETE`/`UPDATE` 前，必须先 `mysqldump` 备份；一条命令的成本，远低于事后恢复的代价。
2. 判断"哪些数据是测试产生的"必须有**权威依据**（如 `order_no` 前缀、时间戳、`mission_id` 是否为空），不能靠推算的等式。
3. 发现判断依据不成立时应立即停止并上报，而不是用第二个假设去修补第一个错误。

**最终处置**：已按用户要求清空业务表；误删前的完整备份保留在 `/tmp/lwg-verify/lwg-backup-before-wipe.sql`（119472 字节，含四张表全部数据）。

### 代码：修复取消任务（撤榜）功能的三个致命缺陷 + 错误处理

改动目的：`MissionController.cancelMission` / `MissionServiceImpl.cancelMission` 自 2026-02-23 编写后从未成功执行过，接口必然返回 500。经实测定位到三个串联的致命缺陷，另补齐参数校验与错误码。

涉及文件：

| 文件 | 改动 |
| --- | --- |
| `src/main/resources/mapper/UserMapper.xml` | `unfreezeBalance` 的表名 `sys_user` → `t_user`，并补 `update_time = NOW()` |
| `src/main/java/com/li/lwg/service/impl/MissionServiceImpl.java` | 补两条 `REFUND` 流水的 `balanceAfter` 快照（解冻后回查用户）；两条流水共用同一个 `orderNo`；`updateCancelInfo` 返回 0 时抛业务异常 |
| `src/main/resources/mapper/MissionMapper.xml` | `updateCancelInfo` 补状态守卫 `AND status = 0` |
| `src/main/java/com/li/lwg/dto/MissionCancelReq.java` | 补 `@NotNull`（missionId/userId）与 `@Size(max = 255)`（cancelReason，与 `t_mission.cancel_reason` 的 varchar(255) 对齐） |
| `src/main/java/com/li/lwg/controller/MissionController.java` | `cancelMission` 的参数注解由 `@Validated` 改为 `@Valid`（前者对 `@RequestBody` 的约束注解不生效，等于没校验），移除随之失效的 import |
| `src/main/java/com/li/lwg/exception/GlobalExceptionHandler.java` | 新增 `MethodArgumentNotValidException` → 400（带首条校验提示）与 `DataIntegrityViolationException` → 400 两个处理器，避免校验失败落到兜底分支返回 500 |
| `pom.xml` | 新增 `spring-boot-starter-validation`（`spring-boot-starter-web` 不含 Bean Validation 实现，缺它 `@NotNull`/`@Size` 无法编译） |

三个致命缺陷的实测证据：

1. **表名错误**：`Table 'lwg.sys_user' doesn't exist`，接口 500、事务整体回滚。此前一直被第 1 个缺陷挡在前面，未暴露。
2. **`balance_after` 缺失**：修好表名后暴露，报 `Column 'balance_after' cannot be null`。`t_transaction_log.balance_after` 为 `NOT NULL`，而取消链路构建两条流水时从未设置该字段，发布与结算链路都已设置。
3. **`@Validated` 不生效 + 无校验约束**：`MissionCancelReq` 原先没有任何约束注解，`@Validated` 对 `@RequestBody` 也不触发校验，因此 `{"userId":1}` 这类请求会穿透到 `selectMissionForUpdate(null)`。

验证（均为实测，测试数据已全部清理）：

- 正常链路：发布 1200 → 资金 `50000/0 → 48800/1200` → 取消 → 回到 `50000/0`；任务 `status=4`、`cancel_reason` 落库；四条流水 `balance_after` 依次为 `48800 / 1200 / 0 / 50000`，发布两条同单号、退回两条同单号且与发布单号不同；
- 参数分支：缺 `missionId` / 缺 `userId` / 两字段全缺 / 原因 300 字符，四种均返回 400 且提示可读；
- 业务分支：任务不存在、重复取消（status=4）均返回 400；他人任务返回「无权越俎代庖」，且任务状态保持 0 未被改动；
- 回归：`mvn test` 全绿（`Tests run: 6, Failures: 0, Errors: 0`）；
- 开发库零残留：`users=2 missions=15 logs=1158 reps=4`，与基线完全一致；冻结账平衡 `7018 = 7018`。

仍未处理（有意留给后续，不属本次修复范围）：

- 前端 `lwg-ui` 没有取消任务入口，`src/api/mission.js` 也没有 `/mission/cancel` 接口，功能目前只能通过 HTTP 直接调用；
- `MissionStatusEnum.getByCode` / `getDescByCode` 仍无人调用，状态文案后端（`进行中`/`待验收`）与前端 `MissionHall.vue`（`修仙中`/`待结算`）不一致；
- 只能取消 `status=0` 的任务，接单人接单后跑路时发布者无退出机制（`deadline` 已存在但无超时处理）。

### 代码（前端仓库 lwg-ui）：补齐撤榜入口

改动目的：后端 `POST /api/mission/cancel` 已可用，但前端没有任何入口，功能对用户不可见。

涉及文件（均为 lwg-ui 仓库）：

| 文件 | 改动 |
| --- | --- |
| `src/api/mission.js` | 新增 `cancelMission(data)`，调用 `POST /mission/cancel` |
| `src/views/MissionHall.vue` | ① 列表「操作」列新增「撤榜」按钮，条件 `queryParams.status === 0 && row.publisherId === myUserId`；② 详情弹窗底部新增「撤榜退回押金」按钮，条件为本人且 `status === 0`；③ 新增 `handleCancel(mission)` / `handleCancelInDetail()`，用 `ElMessageBox.prompt` 收集原因（必填、上限 255 字符，与后端 `@Size(max = 255)` 对齐）后调用接口；④ 新增 `.danger-btn` 样式 |

一处需要注意的实现细节：列表「操作」列原有 `<span v-else-if="queryParams.status !== 0">--</span>`。新增的撤榜按钮若直接插在其前面，会把 `span` 串进同一条条件链，导致「他人发布的待接单任务」既不显示按钮也不显示 `--` 占位。已将该 `span` 改为独立的 `v-if` 并加注释说明。

验证（CDP 驱动真实界面，非仅静态检查）：

- Vite 编译产物中 `cancelMission` / `handleCancel` / `ElMessageBox` / `danger-btn` 等符号均存在，无编译错误；
- 列表：5 条本人待接单任务的「操作」列均为「详情 + 撤榜」；
- 详情弹窗底部按钮为「关闭卷轴 + 撤榜退回押金」；
- 点击撤榜弹出原因输入框，空原因提交被前端拦截并提示「请填写撤榜原因」；
- 填写原因确认后真实取消成功：任务 `status=4`、`cancel_reason` 落库、资金 `199680/7028 → 199690/7018`（押金 10 灵石退回）、两条退回流水共用单号 `5a17d06c` 且 `balance_after` 正确；应用日志确认执行的是 `UPDATE t_mission ... WHERE id = ? AND status = 0`（新增守卫生效）；
- 全程浏览器无 JS 异常；
- 测试数据已恢复：`#51` 还原为 `status=0`、删除两条测试流水、资金还原为 `199680/7028`，冻结账平衡 `7028 = 7028`。

## 2026-09-17

### 代码：阶段 2 测试改用 Testcontainers（消除对开发库的污染）

改动目的：`mvn test` 原本连的是开发者本机数据库，`UserRechargeTest.testConcurrentRecharge` 会真实充值 `100 × 1000 = 100000` 灵石且不回滚，且两个测试类都强依赖本机 MySQL/RabbitMQ 必须在线——这既污染开发数据，也让 CI 无法运行。

涉及文件：

- **新增** `src/test/java/com/li/lwg/AbstractIntegrationTest.java`：测试基类，静态代码块启动一次性 MySQL 与 RabbitMQ 容器，通过 `@DynamicPropertySource` 注入连接信息覆盖 `application-dev.yml` 的本机默认值；显式把 `spring.flyway.baseline-on-migrate` 置回 `false`（容器是全新空库，应真正执行 V1）；显式放大 Hikari 连接池到 50，避免并发测试耗在排队上。
- **新增** `src/test/resources/fixtures/recharge-users.sql`：固定主键（1/2）的用户夹具，幂等（先删后插）。
- **重写** `src/test/java/com/li/lwg/LwgApplicationTests.java`：除 `contextLoads` 外，新增「数据源必须指向测试容器而非开发库」的隔离性断言，以及「Flyway 在全新库执行 V1 并建出四张表」的迁移回归断言。
- **重写** `src/test/java/com/li/lwg/service/UserRechargeTest.java`：不再硬编码 `userId = 1L`；并发数由 1000 降到 100；新增余额精确性、非法金额拒绝、以及「流水条数必须等于成功次数」三类断言。
- **修改** `pom.xml`：新增 `testcontainers-bom`（1.20.4，`dependencyManagement` 统一版本）与 `junit-jupiter`、`mysql`、`rabbitmq` 三个模块（均 test scope）。

三个实现要点：

1. **镜像可覆盖**：默认 `mysql:8.4` / `rabbitmq:3.13`（与生产目标一致），本机 Docker Hub 不可达时可用 `-Dlwg.test.mysql.image=arm64v8/mysql:latest -Dlwg.test.rabbitmq.image=arm64v8/rabbitmq:3.13.7` 改用本地已缓存镜像；非官方镜像名会自动经 `asCompatibleSubstituteFor` 声明兼容，否则 Testcontainers 拒绝启动。
2. **本机需禁用 ryuk**：Testcontainers 的资源回收容器 `testcontainers/ryuk:0.11.0` 同样要从 Docker Hub 拉取，本机不可达时须加环境变量 `TESTCONTAINERS_RYUK_DISABLED=true`；容器仍由 JVM 关闭钩子停止。CI（GitHub Runner）网络正常，无需该变量。
3. **未引入 `@ServiceConnection`**：它需要 `spring-boot-testcontainers` 依赖，而该包在当前网络下无法下载，故采用等价的 `@DynamicPropertySource` 方案。

验证（均为实测）：

- `mvn test` 结果 `Tests run: 6, Failures: 0, Errors: 0`，`BUILD SUCCESS`；
- Testcontainers 日志确认容器真实启动（MySQL 6.1s / RabbitMQ 3.4s），且 Flyway 在容器库上 `Successfully applied 1 migration ... now at version v1`；
- **开发库零变化**：`users=2 missions=15 logs=1158 reps=4`，用户 1 为 `199690/7018/v1105`，与运行前完全一致（改造前该测试会使余额增加十万）；
- 测试结束后无残留容器（`docker ps -a --filter label=org.testcontainers` 为空）。

未执行：未在 GitHub Actions 上运行（阶段 4）。本地使用的镜像是 MySQL 9.2，与生产目标的 8.4 存在版本差异，DDL 已实测在两者均可执行，但 CI 首次跑通前不能视为完全等价。

## 2026-09-17

### 代码：阶段 1 配置外置 + Flyway 纳入表结构（CI/CD 前置）

改动目的：为 CI/CD 做代码可部署化改造。原配置把数据源指向 `localhost`、密码硬编码在 `application.yml`，在容器环境必然连错或连不上；且仓库内没有任何 DDL，服务器无法建库。

涉及文件：

- **新增** `src/main/resources/db/migration/V1__init_schema.sql`：从开发库导出的真实 DDL（`mysqldump --no-data`），整理为 `CREATE TABLE IF NOT EXISTS`、去掉会话级 SET 与 `AUTO_INCREMENT` 计数器值，覆盖 `t_user`、`t_mission`、`t_transaction_log`、`t_reputation_log` 四张表。
- **新增** `src/main/resources/application-dev.yml`：本地开发数据源（`localhost:3306/lwg`）与 RabbitMQ（`127.0.0.1`），并开启 `flyway.baseline-on-migrate`。
- **新增** `src/main/resources/application-prod.yml`：容器环境数据源（`lwg-mysql:3306`）与 RabbitMQ（`lwg-rabbitmq`），`DB_USERNAME`/`DB_PASSWORD` 不设默认值。
- **修改** `src/main/resources/application.yml`：只保留与环境无关的公共配置，新增 `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:dev}`、`spring.flyway`、`management.endpoints`。
- **修改** `pom.xml`：新增 `flyway-core`、`flyway-mysql`、`spring-boot-starter-actuator` 依赖。
- **修改** `.gitignore`：忽略 `.env`。
- **新增** `.env.example`：环境变量模板（仅占位值）。

两处关键决策：

1. **`allowPublicKeyRetrieval=true`** 补进 dev 与 prod 的 JDBC URL。MySQL 8.0+ 默认 `caching_sha2_password`，在 `useSSL=false` 下不加此参数会报 `Public Key Retrieval is not allowed`，导致首个请求 500（本会话已在真实环境复现过）。
2. **`baseline-on-migrate` 只在 dev 打开**：开发库是先有表后引入 Flyway，schema 非空且无 `flyway_schema_history`，必须 baseline 才能启动；生产是全新库，走不到该分支，且刻意保持 `false`，避免误连已有库时静默跳过迁移。

与开发库的两处「有意注释偏差」（仅注释文案，结构与数据不动）：`t_transaction_log.type` 第 4 类改为「悬赏退回」与 `TransactionType.REFUND` 对齐；`t_reputation_log.source_type` 改为「1-任务结算, 2-任务超时」与 `ReputationSourceEnum` 对齐。开发库中残留的旧文案未执行 ALTER。

验证（均为实测）：

- 迁移在**全新临时库**执行成功（退出码 0）、重复执行幂等，且归一化 `AUTO_INCREMENT` 后与开发库结构逐字节一致（仅剩上述 2 处有意注释差异）；
- `mvn spring-boot:run` 默认走 dev，启动 4s，`/actuator/health` 返回 `{"status":"UP"}`（db 与 rabbit 均 UP）；
- 在"有表但无 flyway 元数据"的库上验证 baseline 语义：只生成 `BASELINE (version 1)`，**不重复执行 V1**；
- `SPRING_PROFILES_ACTIVE=prod` 启动按预期失败于 `UnknownHostException: lwg-mysql`，日志中 `localhost:3306/lwg` 出现 0 次，证明 profile 隔离有效；
- 全程**开发库数据零变化**：`users=2 missions=15 logs=1158 reps=4`，与改动前基线完全一致。

未执行：`mvn test`（现有测试依赖真实 MySQL 且会污染开发库，属阶段 2 待改范围）。未改动 `.github/workflows`（阶段 4）。未提交 git。

## 2026-09-16

### 文档：新增 GitHub CI/CD 入门与接入说明

- 新增 `docs/cicd-guide.md`，面向零基础说明 CI/CD 的作用、GitHub Actions 的运行原理（事件、runner、workflow/job/step、action、服务容器、secrets），并给出 LWG 可用的 `ci.yml` 示例。
- 记录接入 CI 的 6 个前置问题：`LwgApplicationTests` 与 `UserRechargeTest` 依赖真实 MySQL、仓库缺少建表 DDL、`UserRechargeTest.testSingleRecharge` 依赖硬编码 `userId = 1L`、`application.yml` 中 `root/123456` 与 `admin/admin123` 硬编码、1000 线程并发测试在 2 核 runner 上不稳定、runner 上无 RabbitMQ 导致 `RabbitConfig` 声明与 `ReputationListener` 连接失败日志。
- 更新 `docs/project-overview.md` 文档索引，补充 `docs/cicd-guide.md` 条目。
- 本次为纯文档改动，**未新增 `.github/workflows`，未修改 `pom.xml`、测试与配置**。

验证：

- 文档改动，未运行测试。已确认仓库当前不存在 `.github/workflows` 目录，主分支为 `master`。

## 2026-04-19

### 文档：整理当前项目整体理解

- 更新 `docs/project-overview.md`，补充项目定位、领域划分、核心能力、数据与消息流、风险点和后续演进方向。
- 将当前会话中对宗门任务系统的理解沉淀到项目总览，便于后续持续维护。

验证：

- 文档改动，未运行测试。

### 文档：完善 Codex 初始化配置

- 更新 `AGENTS.md`，新增 `Codex Startup Requirements`，明确每次启动或开始任务前先阅读 `docs/project-overview.md` 和 `docs/business-flow.md`。
- 更新 `docs/codex-guide.md`，细化启动必读、代码修改后更新 `docs/change-log.md` 的记录要求，以及输出需结合实际类名和方法名。

验证：

- 文档配置改动，未运行测试。

### 文档：建立项目文档体系

- 新增/重整 `docs/project-overview.md`，作为项目整体说明和文档索引。
- 新增 `docs/business-flow.md`，记录任务、用户资金、信誉查询等核心业务链路。
- 新增 `docs/mq-flow.md`，记录 RabbitMQ 生产、路由、消费和当前风险点。
- 新增 `docs/redis-usage.md`，说明当前未使用 Redis，并记录后续接入模板。
- 新增 `docs/es-usage.md`，说明当前未使用 Elasticsearch，并记录后续接入模板。
- 新增 `docs/codex-guide.md`，记录 Codex 协作开发规则。
- 更新 `AGENTS.md`，要求后续工作前优先阅读项目文档并维护变更记录。

验证：

- 文档改动，未运行测试。
