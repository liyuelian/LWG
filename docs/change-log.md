# 变更记录

> 每次完成分析、文档更新或代码修改后，请追加记录。记录应说明改动目的、涉及文件、是否执行验证。

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
