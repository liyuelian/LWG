#!/usr/bin/env bash
# ============================================================================
# LWG 灵务阁 · 部署脚本（在服务器上执行，由自托管 Runner 调用，也可手动运行）
#
# 流程：
#   1. 读取 .env 并校验必需变量
#   2. 记录当前正在运行的镜像引用（按 digest），作为回滚点
#   3. 拉取目标镜像并启动
#   4. 轮询后端 /actuator/health，确认新版本真的起来了
#   5. 失败（拉取失败、启动失败、健康检查超时）→ 自动把两个服务回滚到上一个可用镜像
#
# 用法：
#   ./deploy.sh                        # 用 .env 里的镜像（CI 会写入）
#   BACKEND_IMAGE=... FRONTEND_IMAGE=... ./deploy.sh
#   ./deploy.sh --no-rollback          # 失败时不回滚（排查问题时用）
#
# 退出码：0 成功；1 失败（已尝试回滚）；2 参数/环境错误
# ============================================================================
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ENV_FILE:-${SCRIPT_DIR}/.env}"
COMPOSE_BASE="${SCRIPT_DIR}/compose.yml"
COMPOSE_PROD="${SCRIPT_DIR}/compose.prod.yml"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-150}"   # 后端健康检查最长等待秒数
ROLLBACK_ON_FAILURE=1

for arg in "$@"; do
  case "$arg" in
    --no-rollback) ROLLBACK_ON_FAILURE=0 ;;
    -h|--help) sed -n '2,20p' "$0"; exit 0 ;;
    *) echo "未知参数: $arg" >&2; exit 2 ;;
  esac
done

log()  { printf '\033[1;34m[deploy]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[warn]\033[0m %s\n'   "$*"; }
err()  { printf '\033[1;31m[error]\033[0m %s\n'  "$*" >&2; }

# ---------------------------------------------------------------- 前置检查
[ -f "$ENV_FILE" ]     || { err "缺少 $ENV_FILE（可参考 .env.example 生成）"; exit 2; }
[ -f "$COMPOSE_BASE" ] || { err "缺少 $COMPOSE_BASE"; exit 2; }
[ -f "$COMPOSE_PROD" ] || { err "缺少 $COMPOSE_PROD"; exit 2; }

set -a; # shellcheck disable=SC1090
source "$ENV_FILE"; set +a

for v in DB_USERNAME DB_PASSWORD RABBITMQ_PASSWORD; do
  [ -n "${!v:-}" ] || { err "变量 $v 未在 $ENV_FILE 中设置"; exit 2; }
done

COMPOSE=(docker compose --env-file "$ENV_FILE" -f "$COMPOSE_BASE" -f "$COMPOSE_PROD")

# ------------------------------------------------- 依赖的数据容器是否就绪
for c in lwg-mysql lwg-rabbitmq; do
  status="$(docker inspect -f '{{.State.Health.Status}}' "$c" 2>/dev/null || echo missing)"
  if [ "$status" != "healthy" ]; then
    err "依赖容器 $c 状态为 '$status'（应为 healthy）。请先启动数据服务。" >&2
    exit 2
  fi
done

# ---------------------------------------------------- 记录回滚点（按 digest）
# 用镜像 ID 而不是 tag：tag 可能被覆盖，ID 唯一指向当时的镜像内容
prev_backend="$(docker inspect -f '{{.Image}}' lwg-backend 2>/dev/null || true)"
prev_frontend="$(docker inspect -f '{{.Image}}' lwg-frontend 2>/dev/null || true)"
log "回滚点: backend=${prev_backend:0:19} frontend=${prev_frontend:0:19}"

rollback() {
  [ "$ROLLBACK_ON_FAILURE" -eq 1 ] || { warn "已跳过回滚（--no-rollback）"; return 0; }
  if [ -z "$prev_backend" ] && [ -z "$prev_frontend" ]; then
    warn "没有可回滚的历史镜像（这可能是首次部署）"
    return 0
  fi
  log "开始回滚……"
  if [ -n "$prev_backend" ]; then
    docker tag "$prev_backend" lwg-backend:rollback
    BACKEND_IMAGE="lwg-backend:rollback" "${COMPOSE[@]}" up -d --no-build lwg-backend || warn "后端回滚失败"
  fi
  if [ -n "$prev_frontend" ]; then
    docker tag "$prev_frontend" lwg-frontend:rollback
    FRONTEND_IMAGE="lwg-frontend:rollback" "${COMPOSE[@]}" up -d --no-build lwg-frontend || warn "前端回滚失败"
  fi
  warn "回滚命令已执行，请人工确认服务状态：docker compose ps"
}

on_error() {
  err "部署失败（第 $LINENO 行）"
  rollback
  exit 1
}
trap on_error ERR

# ------------------------------------------------------------- 拉取并启动
# 拉取重试：从国内拉 ghcr.io 时，CDN 偶尔会把连接调度到极慢的节点
# （实测同一层出现 18KB/s 与 11MB/s 两种速度，前者会让 pull 长时间卡住）。
# 单次失败就放弃会导致部署随机失败，因此这里做有限次重试。
PULL_RETRIES="${PULL_RETRIES:-3}"
log "拉取镜像: ${BACKEND_IMAGE:-<未设置>} / ${FRONTEND_IMAGE:-<未设置>}（最多重试 ${PULL_RETRIES} 次）"
pull_ok=0
for attempt in $(seq 1 "$PULL_RETRIES"); do
  if "${COMPOSE[@]}" pull; then
    pull_ok=1
    log "镜像拉取成功（第 ${attempt} 次尝试）"
    break
  fi
  warn "第 ${attempt}/${PULL_RETRIES} 次拉取失败"
  [ "$attempt" -lt "$PULL_RETRIES" ] && sleep $((attempt * 10))
done
if [ "$pull_ok" -ne 1 ]; then
  err "镜像拉取连续失败 ${PULL_RETRIES} 次"
  false   # 触发 trap 回滚
fi

log "启动服务"
"${COMPOSE[@]}" up -d --no-build

# ----------------------------------------------------------- 健康检查等待
log "等待后端健康检查（最多 ${HEALTH_TIMEOUT}s）"
deadline=$(( $(date +%s) + HEALTH_TIMEOUT ))
while :; do
  status="$(docker inspect -f '{{.State.Health.Status}}' lwg-backend 2>/dev/null || echo missing)"
  case "$status" in
    healthy) log "后端已健康"; break ;;
    unhealthy)
      err "后端容器进入 unhealthy 状态"
      docker logs --tail 40 lwg-backend || true
      false ;;   # 触发 trap 回滚
  esac
  if [ "$(date +%s)" -ge "$deadline" ]; then
    err "健康检查超时（当前状态: $status）"
    docker logs --tail 40 lwg-backend || true
    false
  fi
  sleep 5
done

# --------------------------------------------------------- 部署结果汇总
log "部署完成"
"${COMPOSE[@]}" ps

# 打印实际生效的版本与来源，便于核对与归档
for c in lwg-backend lwg-frontend; do
  img="$(docker inspect -f '{{.Config.Image}}' "$c" 2>/dev/null || echo '?')"
  dig="$(docker inspect -f '{{index .Config.Labels "org.opencontainers.image.revision"}}' "$c" 2>/dev/null || echo '')"
  printf '  %-14s image=%s revision=%s\n' "$c" "$img" "${dig:-未知}"
done

# 从站点端口做一次真实探活（验证 nginx→backend→mysql 整条链路）
port="${FRONTEND_PORT:-8081}"
if command -v curl >/dev/null 2>&1; then
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "http://127.0.0.1:${port}/actuator/health" || echo 000)"
  if [ "$code" = "200" ]; then
    log "站点探活通过: http://127.0.0.1:${port}/actuator/health → 200"
  else
    warn "站点探活返回 $code（容器健康但站点访问异常，请检查 nginx 与端口映射）"
  fi
fi

trap - ERR
