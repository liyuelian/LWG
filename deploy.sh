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
COMPOSE_DIR="${COMPOSE_DIR:-$SCRIPT_DIR}"
ENV_FILE="${ENV_FILE:-${COMPOSE_DIR}/.env}"
COMPOSE_BASE="${COMPOSE_BASE:-${COMPOSE_DIR}/compose.yml}"
COMPOSE_PROD="${COMPOSE_PROD:-${COMPOSE_DIR}/compose.prod.yml}"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-150}"   # 后端健康检查最长等待秒数
ROLLBACK_ON_FAILURE=1

# COMPOSE_DIR 允许覆盖：CI 里 compose 文件来自 workflow 的检出目录，
# 而生产凭据（.env）只存在于服务器的 /opt/lwg，因此需要分别指定：
#   COMPOSE_DIR=<检出的仓库目录> ENV_FILE=/opt/lwg/.env ./deploy.sh

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

# 环境变量优先级：外部传入（如 CI 指定的镜像 tag）> .env 文件。
# 做法：先把外部值收集起来，source 之后再覆盖回去。
#
# 为什么必须这么做：source 会无条件覆盖同名变量。若不处理，.env 里的
# BACKEND_IMAGE=latest 会把 CI 传进来的 sha tag 冲掉，导致"指定版本部署"
# 静默失效——实际部署了 latest，却以为部署了 sha，日志里也看不出异常。
_OVERRIDE_NAMES=(BACKEND_IMAGE FRONTEND_IMAGE DB_NAME DB_USERNAME DB_PASSWORD
                 RABBITMQ_USERNAME RABBITMQ_PASSWORD FRONTEND_PORT JAVA_OPTS PULL_POLICY)
_OVERRIDE_VALUES=()
for _i in "${!_OVERRIDE_NAMES[@]}"; do
  _OVERRIDE_VALUES+=("${!_OVERRIDE_NAMES[_i]:-}")
done

set -a; # shellcheck disable=SC1090
source "$ENV_FILE"; set +a

for _i in "${!_OVERRIDE_NAMES[@]}"; do
  if [ -n "${_OVERRIDE_VALUES[_i]}" ]; then
    export "${_OVERRIDE_NAMES[_i]}=${_OVERRIDE_VALUES[_i]}"
  fi
done
unset _i _OVERRIDE_NAMES _OVERRIDE_VALUES

for v in DB_USERNAME DB_PASSWORD RABBITMQ_PASSWORD; do
  [ -n "${!v:-}" ] || { err "变量 $v 未在 $ENV_FILE 中设置"; exit 2; }
done

# 刻意不使用 `docker compose --env-file`：该选项会让 compose 以 .env 覆盖
# shell 环境变量，从而使上面恢复的外部传参失效。改为把 .env 读进 shell 环境
# （已在上面完成），compose 便会直接采用当前环境变量，优先级符合预期。
COMPOSE=(docker compose -f "$COMPOSE_BASE" -f "$COMPOSE_PROD")

# ------------------------------------------------- 依赖的数据容器是否就绪
for c in lwg-mysql lwg-rabbitmq; do
  status="$(docker inspect -f '{{.State.Health.Status}}' "$c" 2>/dev/null || echo missing)"
  if [ "$status" != "healthy" ]; then
    err "依赖容器 $c 状态为 '$status'（应为 healthy）。请先启动数据服务。" >&2
    exit 2
  fi
done

# ---------------------------------------------------- 记录回滚点（按 digest）
# 用镜像 ID 而不是 tag：tag 可能被覆盖，ID 唯一指向当时的镜像内容。
# 同时读取「上一次成功部署」的参数快照——回滚必须复用那份参数，而不是沿用
# 本次失败部署的参数。实测教训：若本次 JAVA_OPTS 是坏的（例如 -Xmx1k 导致
# JVM 无法启动），回滚时继续带着它，旧镜像同样起不来，回滚形同虚设。
prev_backend="$(docker inspect -f '{{.Image}}' lwg-backend 2>/dev/null || true)"
prev_frontend="$(docker inspect -f '{{.Image}}' lwg-frontend 2>/dev/null || true)"
STATE_FILE="${STATE_FILE:-${COMPOSE_DIR}/deploy.state}"

declare -A PREV_ARGS=()
if [ -f "$STATE_FILE" ]; then
  while IFS='=' read -r k v; do
    [ -z "$k" ] && continue
    case "$k" in \#*) continue ;; esac
    PREV_ARGS["$k"]="$v"
  done < "$STATE_FILE"
  log "已加载上次成功部署的参数快照: $STATE_FILE（${#PREV_ARGS[@]} 项）"
else
  warn "无参数快照 $STATE_FILE，回滚将只能恢复镜像、无法恢复运行参数"
fi

log "回滚点: backend=${prev_backend:0:19} frontend=${prev_frontend:0:19}"

rollback() {
  [ "$ROLLBACK_ON_FAILURE" -eq 1 ] || { warn "已跳过回滚（--no-rollback）"; return 0; }
  if [ -z "$prev_backend" ] && [ -z "$prev_frontend" ]; then
    warn "没有可回滚的历史镜像（这可能是首次部署）"
    return 0
  fi
  log "开始回滚……"
  # 回滚使用「上一次成功部署的参数快照」，而不是本次失败部署的参数：
  # 本次的参数正是导致失败的原因，沿用它旧镜像同样起不来（已实测复现）。
  local rb_env=(env PULL_POLICY=never)
  for k in JAVA_OPTS; do
    if [ -n "${PREV_ARGS[$k]:-}" ]; then
      rb_env+=("$k=${PREV_ARGS[$k]}")
      log "回滚参数 $k=${PREV_ARGS[$k]:0:60}"
    fi
  done
  # 镜像 tag 从快照恢复（镜像内容则由 retag 出来的本地 rollback 标签保证）
  local rb_backend_ref="${PREV_ARGS[BACKEND_IMAGE]:-lwg-backend:rollback}"
  local rb_frontend_ref="${PREV_ARGS[FRONTEND_IMAGE]:-lwg-frontend:rollback}"

  if [ -n "$prev_backend" ]; then
    if docker tag "$prev_backend" lwg-backend:rollback 2>/dev/null; then
      BACKEND_IMAGE="$rb_backend_ref" "${rb_env[@]}" "${COMPOSE[@]}" up -d --no-build lwg-backend \
        || warn "后端回滚启动失败"
    else
      warn "后端旧镜像 retag 失败，跳过回滚"
    fi
  fi
  if [ -n "$prev_frontend" ]; then
    if docker tag "$prev_frontend" lwg-frontend:rollback 2>/dev/null; then
      # 前端 depends_on 后端 health，后端恢复健康后前端才可能起来；
      # 若后端仍在 unhealthy，这一步会失败，属预期（问题根在后端）。
      FRONTEND_IMAGE="$rb_frontend_ref" "${rb_env[@]}" "${COMPOSE[@]}" up -d --no-build lwg-frontend \
        || warn "前端回滚启动失败（若后端未恢复健康，此项失败属预期）"
    else
      warn "前端旧镜像 retag 失败，跳过回滚"
    fi
  fi
  # 回滚后确认后端确实恢复了
  local rb_status
  rb_status="$(docker inspect -f '{{.State.Health.Status}}' lwg-backend 2>/dev/null || echo missing)"
  if [ "$rb_status" = "healthy" ]; then
    warn "回滚成功，服务已恢复到上一个版本（backend=${prev_backend:0:19}）"
  else
    err "回滚后后端状态为 '$rb_status'，请人工介入：docker compose -f compose.yml -f compose.prod.yml ps"
  fi
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

# ------------------------------------------- 记录「本次成功部署」的参数快照
# 下一次部署若失败，回滚要用这份快照来恢复运行参数——不能沿用失败那次的参数，
# 因为失败的往往正是参数本身（例如坏的 JAVA_OPTS）。
#
# 注意：必须记录【实际生效的值】而不是环境变量的原始值。若这里写空，
# 回滚时 env JAVA_OPTS= 会覆盖 compose 里的默认值，导致 JVM 拿不到内存参数。
DEFAULT_JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError -Djava.net.preferIPv4Stack=true -Duser.timezone=Asia/Shanghai"
umask 077
cat > "$STATE_FILE" <<EOF
# 由 deploy.sh 在每次成功部署后自动写入，供下次回滚使用，请勿手工编辑。
# 更新时间: $(date '+%Y-%m-%d %H:%M:%S')
BACKEND_IMAGE=${BACKEND_IMAGE}
FRONTEND_IMAGE=${FRONTEND_IMAGE}
JAVA_OPTS=${JAVA_OPTS:-$DEFAULT_JAVA_OPTS}
EOF
log "已记录成功部署参数: $STATE_FILE"

trap - ERR
