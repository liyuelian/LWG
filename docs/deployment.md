# 部署说明（阿里云生产环境）

> 本文记录 LWG 在阿里云服务器上的实际部署形态、操作步骤与排查方法。
> 记录时间：2026-09-17。相关脚本：仓库根目录 `deploy.sh`、`compose.yml`、`compose.prod.yml`。

## 1. 服务器概况

| 项 | 值 |
| --- | --- |
| 地址 | `8.163.86.86`（SSH 别名 `ali-root`，见本机 `~/.ssh/config`） |
| 系统 | Ubuntu 22.04.5 LTS，`amd64` |
| 规格 | 2 核 / **1.6G 内存**（未升级）/ 40G 磁盘（可用约 21G） |
| Docker | 29.1.3（Ubuntu 自带 `docker.io` 包）+ `docker-compose-plugin` v5.5.1 |
| 部署目录 | `/opt/lwg` |

### 镜像加速器

服务器直连 Docker Hub 会超时，`/etc/docker/daemon.json` 已配置：

```json
{ "registry-mirrors": ["https://docker.m.daocloud.io", "https://docker.1panel.live"] }
```

`ghcr.io` 从服务器可直连（无需加速器），应用镜像走它。

### 内存兜底

内存仅 1.6G，除原有 4G swap 外新增 `/swapfile2`（2G，已写入 `/etc/fstab`），
并把 `vm.swappiness` 设为 10（降低换出频率，保护 MySQL）。**总 swap 6G**。

## 2. 容器构成

```
┌──────────────────────────────────────────────────────────┐
│ 服务器 8.163.86.86                                        │
│                                                          │
│  对外: 0.0.0.0:8081 ──► lwg-frontend (nginx)              │
│                            │                             │
│                            │ /api 反向代理（容器名解析）    │
│                            ▼                             │
│  仅本机: 127.0.0.1:8080 ──► lwg-backend (Spring Boot)     │
│                            │                             │
│              ┌─────────────┴─────────────┐               │
│              ▼                           ▼               │
│        lwg-mysql:3306            lwg-rabbitmq:5672       │
│        （仅内网）                  （仅内网）              │
└──────────────────────────────────────────────────────────┘
```

四个容器都在自定义 bridge 网络 **`lwg`** 上，互相用**服务名**解析
（这也是 `application-prod.yml` 里 `DB_HOST=lwg-mysql` 的原因）。

### 关键设计：数据容器不由 compose 管理

`compose.yml` 里的 `lwg-mysql` / `lwg-rabbitmq` 只声明在 `provision` profile 下，
**默认不启动**。服务器上这两个容器是用 `docker run` 手工创建的，原因：

- 避免 compose 误删或重建数据容器，**保护数据卷**；
- 数据服务与应用服务的生命周期本就不同（前者常驻，后者每次部署替换）。

因此 `docker compose ps` 只显示 `lwg-backend` 与 `lwg-frontend`，这是**正常现象**。
查看全部容器请用 `docker ps`。

数据落盘在宿主目录（非命名卷），便于直接备份：

- MySQL：`/opt/lwg/data/mysql`
- RabbitMQ：`/opt/lwg/data/rabbitmq`

## 3. 容器资源限额（依据实测占用设定）

| 容器 | 内存上限 | 实测占用 | 说明 |
| --- | --- | --- | --- |
| lwg-mysql | 320M | ~214M | `innodb-buffer-pool-size=128M`、`performance-schema=OFF`、`max-connections=30` |
| lwg-backend | 384M | ~221M | `-XX:MaxRAMPercentage=70`、`-XX:+UseSerialGC`、`-XX:MaxMetaspaceSize=128m` |
| lwg-rabbitmq | 192M | ~115M | 未启用管理插件（省约 100M） |
| lwg-frontend | 64M | ~6M | nginx 托管静态资源 |

合计约 556M。部署后实测：**无 OOM、无重启**（`RestartCount=0`、`OOMKilled=false`）。

## 4. 环境变量

生产凭据在 `/opt/lwg/.env`（权限 `600`），**不入库**。模板见仓库 `.env.example`。

| 变量 | 说明 |
| --- | --- |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL 凭据，随机生成 |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | RabbitMQ 凭据，随机生成 |
| `FRONTEND_PORT` | 对外端口，默认 `8081` |
| `BACKEND_IMAGE` / `FRONTEND_IMAGE` | 应用镜像；CD 部署时覆盖为具体 commit sha |

## 5. 部署流程

### 手工部署

```bash
cd /opt/lwg
./deploy.sh                      # 读 .env 里的镜像
./deploy.sh --no-rollback        # 失败时不回滚，便于排查
BACKEND_IMAGE=ghcr.io/liyuelian/lwg-backend:<sha> \
FRONTEND_IMAGE=ghcr.io/liyuelian/lwg-frontend:<sha> ./deploy.sh
```

`deploy.sh` 的行为：

1. 校验 `.env` 必需变量与依赖容器健康状态（`lwg-mysql` / `lwg-rabbitmq` 必须 healthy）；
2. 记录当前运行镜像的**镜像 ID** 作为回滚点；
3. 拉取目标镜像（**失败自动重试 3 次，递增退避**）；
4. `docker compose up -d`；
5. 轮询 `lwg-backend` 的 `/actuator/health`（默认最长 150s）；
6. 任一步失败 → 用回滚点镜像重启服务，并以退出码 1 结束；
7. 成功则打印实际镜像、commit revision，并从站点端口做一次端到端探活。

### 首次部署已验证

Flyway 在全新库上正确执行：

```
Current version of schema `lwg`: << Empty Schema >>
Migrating schema `lwg` to version "1 - init schema"
Successfully applied 1 migration to schema `lwg`, now at version v1
```

## 6. 已知坑与排查

### 拉取 ghcr.io 镜像偶发极慢

从国内拉 ghcr.io 时，CDN 可能把连接调度到极慢节点。实测**同一个层**出现过
`18 KB/s` 与 `11 MB/s` 两种速度，前者会让 `docker pull` 长时间卡住
（表现为层显示 `Already exists` 但无进展）。`deploy.sh` 已内置重试。

若仍卡住，排查步骤：

```bash
docker ps -a                       # 是否已有半成品容器
du -sm /var/lib/docker             # 目录是否在增长（不增长即卡住）
pkill -9 -f "docker pull"; systemctl restart docker   # 清理后重试
```

### `--build-arg` 与多阶段 FROM

`ARG` 声明在 `FROM` 之前属全局作用域，跨到下一个 `FROM` 需要**重新声明**才能使用。
本项目的 Dockerfile 因此改用字面量镜像名，避免该陷阱。

### MySQL 认证插件

MySQL 8.0+ 默认 `caching_sha2_password`，在 `useSSL=false` 下必须带
`allowPublicKeyRetrieval=true`，否则报 `Public Key Retrieval is not allowed`。
三个 profile 的 JDBC URL 都已包含该参数。

### 安全组

对外仅需放行 **8081/TCP**。后端 8080 只绑 `127.0.0.1`，数据库与 MQ 不映射到宿主机。

## 7. 数据现状

| 表 | 记录数 | 说明 |
| --- | --- | --- |
| `t_user` | 2 | `发布者_老祖`(id=1, 大乘期, 100000) 与 `接单者_张三`(id=2, 化神期, 20000)，为端到端验证而建 |
| `t_transaction_log` | 2 | 上述初始资金对应的**充值流水**（见下方说明） |
| `t_mission` / `t_reputation_log` | 0 | 全新 |
| `flyway_schema_history` | 1 | V1 已应用 |

登录页只需输入用户 ID（`1` 或 `2`），**没有密码校验**——用户身份目前完全靠请求参数传递，
属练手阶段的已知简化，不是部署问题。

### 初始资金必须配套流水（重要）

首次创建测试用户时，直接 `INSERT` 了 `balance` 却**没有写对应的资金流水**，导致用户余额
在流水表中没有来源记录：财务对账页显示 `totalIncome = 0` 而余额为 100000，无法解释。
当次对账检查也直接报「不平」（余额 100000 vs 流水净额 0）。

修复方式是**补记充值流水**（而不是扣减用户余额）：

```sql
INSERT INTO lwg.t_transaction_log
  (user_id, mission_id, amount, balance_after, type, asset_type, order_no, remark, create_time)
VALUES (1, NULL, 100000, 100000, 5, 1, 'SEED...', '初始灵石注入（环境初始化，补记流水）', NOW());
```

修复后对账全部为平，财务概览正确显示 `totalIncome: 100000`。

**规范**：任何以 SQL 直接注入用户余额的场景，都必须同时写入一条 `type=5`（灵石充值）
的流水，`balance_after` 填注入后的真实余额。否则对账等式
「余额 = 可用钱包流水净额」不成立，财务页面会出现无法解释的差额。

> 这个错误在本项目的开发阶段已犯过一次（见 `docs/change-log.md` 的事故记录），
> 本次在生产环境重复了同一错误。已在此固化为规范。

