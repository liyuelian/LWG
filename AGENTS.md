# Repository Guidelines

## Codex Startup Requirements

At the start of every Codex session or task, read these files before analysis or edits:

- `docs/project-overview.md`
- `docs/business-flow.md`

For any code change, also consult and then update `docs/change-log.md` after the change is complete. Responses should refer to concrete project symbols such as `MissionServiceImpl.auditMission`, `UserMapper.freezeBalance`, or `MissionController.publish` instead of generic descriptions.

## Project Structure & Module Organization

This is a single-module Spring Boot project under `com.li.lwg`.

- `src/main/java/com/li/lwg/controller`: REST API entry points.
- `src/main/java/com/li/lwg/service` and `service/impl`: business interfaces and orchestration.
- `src/main/java/com/li/lwg/mapper`: MyBatis mapper interfaces.
- `src/main/resources/mapper`: MyBatis XML SQL mappings.
- `src/main/java/com/li/lwg/entity`: database entities.
- `src/main/java/com/li/lwg/dto` and `vo`: request and response objects.
- `src/main/java/com/li/lwg/config`: infrastructure configuration, currently RabbitMQ.
- `src/main/java/com/li/lwg/listener`: message consumers.
- `src/test/java`: JUnit 5 Spring Boot tests.

## Build, Test, and Development Commands

- `mvn clean package`: compile, run tests, and build the application jar.
- `mvn test`: run the test suite.
- `mvn spring-boot:run`: start the application locally on the configured port, currently `8080`.

Local runtime dependencies are configured in `src/main/resources/application.yml`: MySQL database `lwg` and RabbitMQ at `127.0.0.1:5672`.

## Coding Style & Naming Conventions

Use Java 17 and standard Spring Boot conventions. Keep controllers thin and place business rules in `service.impl`. Mapper interfaces should match XML namespaces and method ids exactly, for example `MissionMapper.acceptMission` with `MissionMapper.xml` `<update id="acceptMission">`.

Use 4-space indentation. Class names use `UpperCamelCase`; methods and fields use `lowerCamelCase`. DTOs should end with `Req`, view objects with `VO`, and enums with `Enum` when they represent business codes.

## Testing Guidelines

Tests use Spring Boot Test with JUnit 5. Place tests under `src/test/java` with names ending in `Test`, for example `UserRechargeTest`.

Prefer focused service tests for transactional logic, mapper-backed behavior, and concurrency-sensitive paths such as balance updates. Run `mvn test` before submitting changes.

## Commit & Pull Request Guidelines

The existing history uses short conventional prefixes such as `feat:`, `fix:`, `test:`, and `refactor:`. Follow that style, for example:

- `feat: add mission timeout handling`
- `fix: correct user balance unfreeze SQL`

Pull requests should include a short summary, affected APIs or tables, test results, and any required MySQL/RabbitMQ setup changes. Link related issues when available.

## Agent-Specific Instructions

Before starting analysis or code changes, read the startup documents listed above. If recent history or prior changes matter, also read:

- `docs/change-log.md`

Follow `docs/codex-guide.md`: analyze before editing, keep changes minimal, preserve existing behavior and style, and ask when requirements are unclear. After any code change, update `docs/change-log.md`; if the change affects business flow, MQ, Redis, ES, or project structure, update the matching document under `docs/`.

## Architecture Notes

The main business flow is `Controller -> ServiceImpl -> Mapper/XML -> MySQL`. Task settlement also publishes RabbitMQ messages through `MissionServiceImpl`, then `ReputationListener` updates reputation records asynchronously. Redis, Elasticsearch, Dubbo, and scheduled jobs are not currently used.
