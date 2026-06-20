# AGENTS.md

Multi-module Maven project under `app/`: **common** (library JAR) → **uam** (Spring Boot application).

**Stack**: Java 25, Spring Boot 4.0.6 / Web MVC (servlet) / JPA + H2 / Liquibase, Lombok, SpringDoc OpenAPI.  
**Branch**: `feature/user-service` (merge target: `develop`).

---

## Commands

All commands run from `app/` using the canonical wrapper:

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

**Canonical `mvnw`**: `app/mvnw`. Do **not** use the stale `app/uam/mvnw`.

**Order matters**: `spotless:check` is bound to `verify`. Run `spotless:apply` first, then `verify`.

---

## Project layout

```
app/
├── pom.xml              — Parent POM (modules, Spotless, dep mgmt)
├── common/              — Library JAR (com.anasdidi.common.*)
│   ├── CommonConstants   — API_V1=/v1, HEADER_CORR_ID=App-Correlation-Id
│   ├── CommonUtils       — ObjectMapper factory (FAIL_ON_UNKNOWN_PROPERTIES=false)
│   ├── config/CommonConfig — @EnableJpaAuditing, ObjectMapper @Bean, AuditorAware
│   ├── entity/BaseEntity  — abstract @MappedSuperclass with UUID id, audit columns, @Version
│   ├── dto/
│   │   ├── BaseReqDTO     — abstract with @NotBlank correlationId
│   │   ├── BaseResDTO     — abstract with traceId, timestamp, responseCode/Desc
│   │   └── PaginationDTO  — pageNo, totalRecordsPerPage, totalPages, totalRecords
│   ├── enums/ResponseEnum — S00_SUCCESS..E99_UNEXPECTED_ERROR (each carries httpStatus, code, message)
│   ├── enums/ResourceEnum — USER
│   ├── service/IBaseService — @Validated: B execute(@Valid A req)
│   ├── aspect/ExecuteTraceAspect — @Around IBaseService.execute() → sets traceId/timestamp/response via MDC
│   └── error/             — ServiceError (abstract), E02/E03/E99UnexpectedError
└── uam/                 — Spring Boot app (com.anasdidi.uam.*)
    ├── UamApplication    — @SpringBootApplication(scanBasePackages="com.anasdidi")
    ├── UamConstants      — CONTEXT_PATH=/uam
    ├── config/UamConfig  — empty @Configuration
    ├── controller/       — interface-first: interface declares @GetMapping etc., impl is @RestController
    │   ├── HelloWorldController (interface)
    │   └── UserController (interface)
    ├── controller/impl/  — HelloWorldControllerV1, UserControllerV1
    ├── service/UamService<A,B> extends IBaseService<A,B>
    ├── service/impl/     — HelloWorldService, RegisterUserService, SearchUserService, GetUserService
    ├── dto/              — extends BaseReqDTO/BaseResDTO with nested payload pattern
    ├── dto/model/        — UserDTO
    ├── entity/UserEntity — extends BaseEntity, maps T_USER
    └── repository/UserRepository — JpaRepository + JpaSpecificationExecutor
```

**Module dependency**: `common` → `uam` (one-way). A change in `common` needs `./mvnw compile -pl uam -am` to rebuild consumers.

---

## Architecture

- **Stack is servlet (not reactive)**: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`. Services are synchronous.
- **Interface-first controllers**: interface class declares `@GetMapping` etc.; `@RestController` impl class adds `@RequestMapping` with the full path.
- **Generic service layer**: `UamService<A, B> extends IBaseService<A, B>` → single `B execute(@Valid A req)`.
- **ExecuteTraceAspect** (`common`) intercepts `IBaseService.execute()` via `@Around`. Sets MDC traceId/spanId/signature, populates `traceId`, `timestamp`, `timeTaken`, `responseCode`/`responseDesc` in `BaseResDTO` on return. Impl services only set `ResponseEnum`.
- **Error handling**: Aspect catches `ConstraintViolationException` → `E01_VALIDATION_ERROR`, `ServiceError` → mapped to its `ResponseEnum` (e.g. `E99UnexpectedError` → `E99_UNEXPECTED_ERROR`), generic `Exception` → `E99_UNEXPECTED_ERROR`.
- API path: `/uam` (`UamConstants.CONTEXT_PATH`) + `/v1` (`CommonConstants.API_V1`) + endpoint path.
- All endpoints require `App-Correlation-Id` header.
- Current endpoints:
  - `GET    /uam/v1/hello-world/greeting?name=...`
  - `POST   /uam/v1/user/register` (body: `RegisterUserReqDTOPayload`)
  - `GET    /uam/v1/user?name=&pageNo=1&totalRecordsPerPage=10`
  - `GET    /uam/v1/user/{userId}`

---

## DTO conventions

- **Request DTOs**: `@Data @SuperBuilder @Jacksonized`, extend `BaseReqDTO` (carries `@NotBlank correlationId`). Use a **nested payload** pattern: inner `@Data @SuperBuilder @Jacksonized` static class field annotated `@Valid @NonNull`.
- **Response DTOs**: `@Data @SuperBuilder @Jacksonized`, extend `BaseResDTO`. Carry `@JsonIgnore ResponseEnum response` (only code/message appear in JSON). Payload field uses `@JsonInclude(Include.NON_NULL)`.
- Correlation ID flows via `App-Correlation-Id` header → controller builds the req DTO with it.
- Both DTOs use `@JsonIgnoreProperties(ignoreUnknown = true)`.

---

## Testing

- **Controller tests**: `@ExtendWith(MockitoExtension.class)` + `MockMvcBuilders.standaloneSetup()` (pure Mockito, no Spring slice loading).
- **Service tests**: `@SpringBootTest` (full context). Tests use H2 in-memory DB (`jdbc:h2:mem:uamdb`) via `src/test/resources/application-test.yml`.
- **Common module**: no `src/test/` — any new tests must be created from scratch.
- **JaCoCo** (uam only): 80% line coverage required for `com.anasdidi.uam.controller.impl` and `com.anasdidi.uam.service.impl` packages (bound to `verify` phase).
- **Security**: `spring-boot-starter-security` (and test counterpart) are **commented out** — app is fully unauthenticated.
- **H2 console** and **Actuator** (unconfigured) are included for development.

---

## Database

- **Liquibase**: Master changelog at `src/main/resources/db/db.changelog-master.xml`. Currently includes one migration creating `T_USER` table (`src/main/resources/db/changelog/db.changelog-t_user_260620.xml`).
- **Main config**: file-based H2 (`jdbc:h2:./h2/portal;AUTO_SERVER=TRUE`), JPA `ddl-auto: validate`.
- **Test config**: in-memory H2 (`jdbc:h2:mem:uamdb`) via `application-test.yml` (activated by `src/test/resources/application.properties`).

---

## Code style

- **Spotless** (parent POM): `palantirJavaFormat` (style=GOOGLE), `removeUnusedImports`, `formatAnnotations`. POM sorted via `sortPOM` (2-space indent). XML resources via Eclipse WTP 4.21.0. YAML via Jackson YAML formatter.
- **EditorConfig**: 2-space indent for Java/XML, LF line endings.
- Do not manually format — use `./mvnw spotless:apply`.

---

## Environment

- Dev container via Docker Compose (`mcr.microsoft.com/devcontainers/java:3-25-trixie`). OpenCode pre-installed.
- Logback writes to `./logs/uam.log` (relative to `app/uam/`), gitignored via root `.gitignore`.
- Spring Boot JVM args: `--enable-native-access=ALL-UNNAMED` (set in parent POM).
- No `.env` file.

---

## Git

- Conventional commits: `feat:`, `fix:`, `refactor:`, `build:`, `chore:`, etc.
- Flow: `develop` ← `feature/*`. No CI workflows exist.

---

## Module-specific AGENTS.md

- [`app/AGENTS.md`](app/AGENTS.md) — command instructions (run from `app/`)
- [`app/common/AGENTS.md`](app/common/AGENTS.md) — common library JAR notes
- [`app/uam/AGENTS.md`](app/uam/AGENTS.md) — uam application notes
