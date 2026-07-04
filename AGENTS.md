# AGENTS.md

Multi-module Maven project under `app/`: **common** (library JAR) → **uam** (Spring Boot app).

**Stack**: Java 25, Spring Boot 4.0.6 / Web MVC (servlet) / JPA + H2 / Liquibase, Lombok, SpringDoc OpenAPI.  
**Branch**: `feature/user-service` (merge target: `develop`).

---

## Commands (run from `app/`)

| What | Command |
|---|---|
| Compile all | `./mvnw compile` |
| Compile uam + deps | `./mvnw compile -pl uam -am` |
| Compile common only | `./mvnw compile -pl common` |
| Run all uam tests | `./mvnw test -pl uam -am` |
| Run single test class | `./mvnw test -pl uam -am -Dtest=HelloWorldControllerV1Tests` |
| Format check | `./mvnw spotless:check` |
| Auto-format | `./mvnw spotless:apply` |
| Full pipeline | `./mvnw verify` |
| Full clean verify | `./mvnw clean verify` |
| Start app | `./mvnw spring-boot:run -pl uam` |

**Canonical `mvnw`**: `app/mvnw`. There's a stale duplicate at `app/uam/mvnw` — do not use it.

**Order matters**: Spotless `check` is bound to `verify`. Run `spotless:apply` first, then `verify`.

---

## Architecture

- **Servlet stack** (not reactive): `spring-boot-starter-web`, `spring-boot-starter-data-jpa`. Services are synchronous.
- **Interface-first controllers**: interface declares `@GetMapping` etc.; `@RestController` impl adds `@RequestMapping` with the full path.
- **Generic service layer**: `UamService<A, B> extends IBaseService<A, B>` → single `B execute(@Valid A req)`.
- **`ExecuteTraceAspect`** (`common`) wraps `IBaseService.execute()` via `@Around`. Sets MDC traceId/spanId/signature, populates `traceId`/`timestamp`/`timeTaken`/response fields in `BaseResDTO`. Catches `ConstraintViolationException` → `E01_VALIDATION_ERROR`, `ServiceError` → mapped `ResponseEnum`, generic `Exception` → `E99_UNEXPECTED_ERROR`.
- **Component scan**: `@SpringBootApplication(scanBasePackages = "com.anasdidi")` picks up `common` components (aspects, config, base classes).
- API path: `/uam` + `/v1` + endpoint path. All endpoints require `App-Correlation-Id` header.
- **`@Transactional` only on write services**: `RegisterUserService`, `UpdateUserService`, `DeleteUserService` (read services like `GetUserService`, `SearchUserService` lack it).
- **Custom repo method**: `UserRepository.findByIdAndVersion(UUID, Integer)` — used by update and delete for optimistic locking.
- **Soft delete**: `DeleteUserService` sets `isDeleted = true` (no physical row removal).
- **Name uppercasing**: `RegisterUserService` and `UpdateUserService` uppercase `name` via `name.toUpperCase()`.

### Endpoints

| Method | Path | Service |
|---|---|---|
| `GET` | `/uam/v1/hello-world/greeting?name=` | `HelloWorldService` |
| `POST` | `/uam/v1/user/register` | `RegisterUserService` |
| `GET` | `/uam/v1/user?name=&pageNo=1&totalRecordsPerPage=10` | `SearchUserService` |
| `GET` | `/uam/v1/user/{userId}` | `GetUserService` |
| `PATCH` | `/uam/v1/user/{userId}?version=` | `UpdateUserService` |
| `DELETE` | `/uam/v1/user/{userId}?version=` | `DeleteUserService` |

---

## DTO conventions

- **All DTOs**: `@Data @SuperBuilder @Jacksonized`, extend `BaseReqDTO`/`BaseResDTO`, with `@JsonIgnoreProperties(ignoreUnknown = true)`.
- **Nested payload pattern**: Every request carries a `@Valid @NonNull` inner static class `Payload` field. Exceptions: `UpdateUserReqDTO` has `userId`/`version` at the DTO level alongside the payload; `DeleteUserReqDTO` nests `userId`/`version` inside the payload.
- **Response DTOs**: Carry `@JsonIgnore ResponseEnum response` — only `code`/`message` appear in JSON. Payload field uses `@JsonInclude(Include.NON_NULL)`.
- Correlation ID flows from `App-Correlation-Id` header → controller builds req DTO.

---

## Testing

- **Controller tests**: `@ExtendWith(MockitoExtension.class)` + `MockMvcBuilders.standaloneSetup()` (pure Mockito, no Spring slice).
- **Service tests**: `@SpringBootTest` (full context). In-memory H2 (`jdbc:h2:mem:uamdb`) via `application-test.yml`.
- **Common module**: no `src/test/` — any new tests must be created from scratch.
- **JaCoCo** (uam only, `verify` phase): ≥80% line coverage enforced for `com.anasdidi.uam.controller.impl` and `com.anasdidi.uam.service.impl`.
- **Security**: `spring-boot-starter-security` (and test counterpart) commented out — fully unauthenticated.
- **H2 console** and **Actuator** (unconfigured) included for development.

---

## Database

- **Liquibase**: `db/db.changelog-master.xml` → `db/changelog/db.changelog-t_user_260620.xml` (creates `T_USER`).
- **Main config**: file-based H2 (`jdbc:h2:./h2/portal`), JPA `ddl-auto: validate`.
- **Test config**: in-memory H2 (`jdbc:h2:mem:uamdb`) via `application-test.yml` (activated by `src/test/resources/application.properties`).

---

## Code style

- **Spotless**: `palantirJavaFormat` (style=GOOGLE), `removeUnusedImports`, `formatAnnotations`. POM sorted via `sortPOM` (2-space). XML via Eclipse WTP 4.21.0. YAML via Jackson.
- **EditorConfig**: 2-space indent for Java/XML, LF endings.
- Do not manually format — use `./mvnw spotless:apply`.

---

## Environment

- Dev container: Docker Compose (`mcr.microsoft.com/devcontainers/java:3-25-trixie`). OpenCode pre-installed.
- Logback writes to `./logs/uam.log` (relative to `app/uam/`), gitignored via root `.gitignore`.
- Spring Boot JVM arg: `--enable-native-access=ALL-UNNAMED` (parent POM).
- No `.env` file.

---

## Git

- Conventional commits: `feat:`, `fix:`, `refactor:`, `build:`, `chore:`, etc.
- Flow: `develop` ← `feature/*`. No CI.

---

## Module-specific AGENTS.md

- [`app/AGENTS.md`](app/AGENTS.md) — commands (run from `app/`)
- [`app/common/AGENTS.md`](app/common/AGENTS.md) — common library notes
- [`app/uam/AGENTS.md`](app/uam/AGENTS.md) — uam application notes
