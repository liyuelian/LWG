# ============================================================================
# LWG 灵务阁 · 后端运行镜像（多阶段构建）
#
# 构建阶段用 Maven + JDK 编译打包，运行阶段只保留 JRE 与 jar：
# 实测镜像 293MB，而构建阶段基础镜像本身就有 536MB，产物不带 Maven 与源码。
#
# 构建：
#   docker build -t lwg-backend:local .
#   （CI 中由 GitHub Actions 构建并推送到镜像仓库，见 .github/workflows）
# ============================================================================

# ---------- 构建阶段 ----------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先只复制 pom.xml 并预下载依赖：源码变更时这一层可复用缓存，显著加快重复构建
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 再复制源码并打包（跳过测试：单元测试由 CI 的独立步骤负责，不重复跑）
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre-jammy

# 构建信息写入 OCI 标准标签：部署后可用 docker inspect 直接确认"线上跑的是哪个提交"，
# 无需登录服务器翻日志。由 CI 通过 --build-arg 传入（见 build-push-action 的 build-args）。
ARG GIT_COMMIT=unknown
ARG BUILD_TIME=unknown
LABEL org.opencontainers.image.revision="${GIT_COMMIT}" \
      org.opencontainers.image.created="${BUILD_TIME}" \
      org.opencontainers.image.title="lwg-backend" \
      org.opencontainers.image.description="LWG 灵务阁后端"
# HEALTHCHECK 需要 curl 访问 actuator；顺带清理 apt 缓存减小体积
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# 非 root 运行，降低容器逃逸风险
RUN groupadd -r lwg && useradd -r -g lwg -d /app -s /usr/sbin/nologin lwg

WORKDIR /app

COPY --from=build --chown=lwg:lwg /build/target/*.jar /app/app.jar

USER lwg

EXPOSE 8080

# JVM 参数：容器感知内存上限、优先使用 IPv4、固定时区
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.net.preferIPv4Stack=true -Duser.timezone=Asia/Shanghai" \
    SPRING_PROFILES_ACTIVE=prod

# 探活：compose 的 depends_on 条件与 deploy.sh 的发布校验都依赖此接口
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
