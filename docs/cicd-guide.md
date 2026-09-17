# GitHub CI/CD 入门与接入说明

> 本文面向零基础读者，说明 CI/CD 是什么、GitHub Actions 的具体运行原理，以及 LWG 项目接入时需要注意的真实情况。
> 截至本文写作时，仓库 `https://github.com/liyuelian/LWG.git` 中**尚未存在** `.github/workflows` 目录，因此本文属于**教学与方案文档，未改动任何代码、未新增任何流水线**。相关背景见 `docs/project-overview.md`，改动记录见 `docs/change-log.md`。

## 1. 为什么需要 CI/CD

当前的开发流程是：

```text
本地改代码 -> 本地执行 mvn test（有时忘记） -> git push -> 结束
```

问题在于「我电脑上能跑」不等于「代码是对的」。本地成功隐含了大量未写下来的前提：本机装了 MySQL、库里有数据、`application.yml` 指向 `localhost`、JDK 版本正好是 17。一旦换一台机器，这些前提全部消失。

CI/CD 的本质是：**把「运行检查」这件事从人工执行变成服务器上自动执行，并且每次代码变动都触发。**

它的价值不是省事，而是**让错误在 3 分钟内暴露，而不是在 3 天后上线时暴露**。

## 2. CI 与 CD 的定义

| 缩写 | 全称 | 人话 |
| --- | --- | --- |
| CI | Continuous Integration 持续集成 | 每次 push，自动拉代码 + 编译 + 跑测试，失败立即通知 |
| CD | Continuous Delivery 持续交付 | CI 通过后自动产出可部署的包，部署动作由人点确认 |
| CD | Continuous Deployment 持续部署 | CI 通过后自动发布到服务器，全程无人值守 |

「持续」的含义是**每次提交都做一遍**，而不是攒到发版前做一次。频率高，反馈才快。

## 3. GitHub Actions 的核心原理

### 3.1 整体流程

```text
你的电脑                  GitHub                     临时云主机 (runner)
────────                ────────                   ─────────────────────
git push  ─────────▶   ① 收到 push 事件
                         ② 在仓库中查找
                            .github/workflows/*.yml
                         ③ 按文件中的剧本
                            开一台全新 Ubuntu 机器 ─▶ ④ checkout 拉取代码
                                                        ⑤ 安装 JDK 17
                                                        ⑥ mvn clean verify
                                                        ⑦ jar 存为产物 artifact
                         ⑨ 在 commit / PR 上标记 ✅ 或 ❌ ◀─┘
```

### 3.2 四个核心概念

| 概念 | YAML 字段 | 作用 |
| --- | --- | --- |
| 事件 | `on:` | 触发器，如 `push`、`pull_request`、`workflow_dispatch`（网页手动按钮）。原理是仓库 webhook |
| Runner | `runs-on:` | 一台**全新、用完即扔**的云主机，是流水线真正执行的机器 |
| Workflow / Job / Step | 文件结构 | 剧本（一个 yml）-> 一场戏（job）-> 一个个步骤（step）。**任一步骤失败，后续全部中断，整体标红** |
| Action | `uses:` | Marketplace 上别人写好的现成步骤，如 `actions/checkout@v4` 表示「把仓库代码拉到 runner 上」 |

### 3.3 三个最容易困惑的原理点

1. **Pipeline as Code（流水线即代码）**：为什么配置放在仓库的 `.github/workflows/`？因为这样流水线和代码同版本、同 review、同回滚。修改 CI 配置本身也是一次 commit。
2. **Runner 是无状态的**：它不是你的电脑，没有任何你装过的软件，也没有本地数据库，上一次运行的一切都已消失。因此两个连锁设计必然出现：每次都要 `actions/checkout` 重新拉代码；Maven 依赖每次都要重下，必须显式加缓存（`actions/setup-java@v4` 的 `cache: maven`）。
3. **结果会回写到提交与 PR**：commit 和 PR 上会出现绿勾 / 红叉。在仓库 Settings -> Branches 中开启 **Require status checks to pass** 后，CI 失败将**禁止合并**。此时 CI 才从「建议」变成真正的「门禁」。

## 4. workflow 文件结构拆解

```yaml
name: LWG CI                 # 流水线名称，显示在 Actions 页面

on:                          # ① 事件：什么时候触发
  push:
    branches: [ master ]     # 推到 master 时触发（本项目主分支为 master）
  pull_request:
    branches: [ master ]     # 针对 master 的 PR 时触发
  workflow_dispatch:         # 允许在网页上手动点按钮触发

jobs:                        # ② 一个流水线可以包含多个 job
  build-and-test:            # job 的 id
    runs-on: ubuntu-latest   # ③ runner：GitHub 提供的临时 Ubuntu 机器

    services:                # ④ 服务容器：在 runner 旁边额外起的 Docker 容器
      mysql:
        image: mysql:8.0
        # ...

    steps:                   # ⑤ 步骤：按顺序执行，失败即中断
      - name: 拉代码
        uses: actions/checkout@v4
      - name: 跑测试
        run: mvn -B clean verify
```

关于 `mvn -B`：`-B` 表示 batch mode，去掉进度条和 ANSI 颜色，让 CI 日志干净，几乎是标配。

## 5. LWG 接入示例：一份可用的 ci.yml

本项目最该自动化的命令就是 `mvn clean verify`（编译 -> 运行 `LwgApplicationTests`、`UserRechargeTest` -> 打成 jar）。

```yaml
name: LWG CI

on:
  push:
    branches: [ master ]
  pull_request:
    branches: [ master ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    # 关键：runner 上没有 MySQL，用服务容器现开一个
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: 123456
          MYSQL_DATABASE: lwg
        ports:
          - 3306:3306
        options: >-
          --health-cmd="mysqladmin ping -h 127.0.0.1 -uroot -p123456"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=10

    steps:
      - name: 拉代码
        uses: actions/checkout@v4

      - name: 安装 JDK 17（带 Maven 缓存）
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: temurin
          cache: maven

      - name: 建表
        run: mysql -h 127.0.0.1 -uroot -p123456 lwg < src/test/resources/schema.sql

      - name: 跑测试并打包
        env:
          # 用环境变量覆盖 application.yml 中的本地配置
          SPRING_DATASOURCE_URL: jdbc:mysql://127.0.0.1:3306/lwg?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false
          SPRING_DATASOURCE_USERNAME: root
          SPRING_DATASOURCE_PASSWORD: 123456
        run: mvn -B clean verify

      - name: 上传 jar 产物
        uses: actions/upload-artifact@v4
        with:
          name: lwg-jar
          path: target/*.jar
```

需要理解的关键点：

- `services:` 由 GitHub 在 runner 旁额外启动一个 Docker 容器，并把 `3306` 映射到 runner 本机，因此 job 内用 `127.0.0.1:3306` 即可连接。
- `SPRING_DATASOURCE_PASSWORD` 这类大写环境变量可以覆盖 `application.yml` 中的 `spring.datasource.password`，这是 Spring Boot 的**宽松绑定（relaxed binding）**机制。这是「同一份代码，本地连本地库、CI 连 CI 库」的标准做法，无需改代码。
- `actions/upload-artifact@v4` 把 jar 存到 GitHub，运行结束后可在网页下载，这是 CD 的原料。
- 生产项目中密码应写为 `${{ secrets.MYSQL_PASSWORD }}`，在仓库 Settings -> Secrets and variables -> Actions 中配置，日志会自动打码。
- 提示：`ubuntu-latest` 镜像通常自带 `mysql` 客户端；若某次运行提示 `command not found`，可先 `sudo apt-get install -y mysql-client`，或改用 `docker exec` 在容器内执行 SQL。

## 6. LWG 当前接入必须解决的前置问题

以下均基于仓库当前真实代码，是接入 CI 后必然踩到的坑。

| # | 问题 | 现状证据 | 影响 |
| --- | --- | --- | --- |
| 1 | 测试强依赖真实数据库 | `LwgApplicationTests`、`UserRechargeTest` 均为 `@SpringBootTest`，后者直接 `@Autowired UserMapper` | runner 上无 MySQL，DataSource 初始化失败，**首次运行必然失败** |
| 2 | 仓库中没有建表 SQL | `docs/project-overview.md` 第 181 行已记录「没有看到数据库 DDL」 | CI 中库为空，SQL 报 `Table 'lwg.t_user' doesn't exist` |
| 3 | 测试数据依赖硬编码 id | `UserRechargeTest.testSingleRecharge` 查询写死的 `userId = 1L` | 空库返回 `null`，`user.getUsername()` 触发 NPE |
| 4 | 凭据硬编码且已进入 git 历史 | `application.yml` 中 `root/123456`、RabbitMQ `admin/admin123` | 应改为 secrets；已提交的密码须视为已泄露，删文件无法抹除历史 |
| 5 | 重量级并发测试在 CI 上可能不稳定 | `UserRechargeTest.testConcurrentRecharge` 使用 1000 线程打 MySQL | 免费 runner 通常仅 2 核，可能超时或抖动 |
| 6 | runner 上没有 RabbitMQ | `RabbitConfig` 声明 exchange/queue/binding，`ReputationListener` 监听队列 | 一般不至于令上下文启动失败，但会输出大量 connection refused 日志，行为不稳定 |

对应的处理建议：

1. 为 MySQL 增加 `services:`（见第 5 节示例）。
2. 从本地导出建表语句放入仓库，例如 `mysqldump --no-data lwg > src/test/resources/schema.sql`。
3. 准备最小测试数据（`data.sql`），或更规范地改为测试内自建数据并配合 `@Transactional` 自动回滚。
4. 密码迁移到 GitHub Secrets，通过 `SPRING_DATASOURCE_PASSWORD` 等环境变量注入。
5. 给并发测试加 `@Tag("heavy")`，CI 中通过 `mvn -B clean verify -DexcludedGroups=heavy` 排除，仅在手动触发时运行。
6. 同样为 RabbitMQ 增加 service 容器，保持日志干净。

## 7. 从 CI 到 CD

CI 到「自动打包」即结束。真正的部署还需要额外步骤：

```text
mvn verify ✅ -> 用 Dockerfile 构建镜像 -> push 到 GHCR / 镜像仓库
              -> SSH 到服务器 -> docker pull && docker run
```

建议的推进顺序：

1. 先让 CI 稳定通过（补齐建表脚本、测试数据、必要的 service 容器）。
2. 再加入「打包 + 上传 artifact」（第 5 节示例已包含）。
3. 最后才考虑 `docker build` 与部署到服务器。

不要一上来就做 CD：它需要服务器、SSH 密钥、镜像仓库凭据等 secrets，风险高、排错难。

## 8. 落地步骤建议

1. 导出 DDL 到 `src/test/resources/schema.sql`（可另加 `data.sql`）。
2. 新建 `.github/workflows/ci.yml`，内容参考第 5 节。
3. `git push` 后打开仓库 **Actions** 标签页，点进本次运行，展开变红的步骤查看 Java 异常栈。
4. 反复修正直到稳定通过。
5. 在 Settings -> Branches 开启 **Require status checks to pass**，把 CI 变成合并门禁。
6. 再考虑 Docker 与 CD。

## 9. 常见问题

| 问题 | 回答 |
| --- | --- |
| 要收费吗？ | 公开仓库使用标准 runner 基本免费不限量；私有仓库 Free 套餐有每月额度（约 2000 分钟） |
| 为什么本地能过 CI 却失败？ | 本地环境的前提（数据库、数据、环境变量）在 CI 上必须显式声明，这正是 CI 最大的价值 |
| 流水线会改我的代码吗？ | 不会。runner 是临时的，除非显式配置 token 回写，否则不影响仓库 |
| secrets 安全吗？ | 日志中自动打码；注意来自 fork 的 PR 默认拿不到 secrets |
| 在哪里看日志？ | 仓库 Actions 标签页 -> 选择某次运行 -> 展开具体 step |
| 怎么手动触发一次？ | `on:` 中配置 `workflow_dispatch`，之后在 Actions 页面点 Run workflow |
| 连续 push 会重复跑吗？ | 会。可用 `concurrency` 配置取消同一分支上未完成的旧运行 |

## 10. 常用 Action 速查

| Action | 用途 |
| --- | --- |
| `actions/checkout@v4` | 把仓库代码拉到 runner |
| `actions/setup-java@v4` | 安装指定 JDK，支持 `cache: maven` 缓存依赖 |
| `actions/upload-artifact@v4` | 上传构建产物，供下载或后续 job 使用 |
| `actions/download-artifact@v4` | 在后续 job 中下载产物 |
| `actions/cache@v4` | 通用缓存，例如缓存 `~/.m2/repository` |

## 11. 与当前项目的关系

本文只做原理说明与接入方案沉淀，**未新增 `.github/workflows`、未修改 `pom.xml`、未修改任何测试或配置**。文档层面已完成：

- `docs/project-overview.md`：文档索引已补充本文链接。
- `docs/change-log.md`：已记录本次文档改动。

后续实际接入 CI 时仍需同步：

- `docs/project-overview.md`：技术栈与基础设施表新增 CI/CD 一行，并在「当前未发现实际使用」中移除相应条目。
- `docs/change-log.md`：记录流水线、建表脚本与测试改造的改动。
