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

**Order matters**: Spotless `check` is bound to `verify` phase. Run `spotless:apply` first, then `verify`.

---

## Project layout

```
app/
├── pom.xml              — Parent POM (modules, Spotless, dep mgmt)
├── common/              — Library JAR (com.anasdidi.common.*)
│   ├── CommonConstants   — CONTEXT_PATH=/uam, API_V1=/v1, HEADER_CORR_ID
│   ├── BaseReqDTO        — abstract @Data class with @NotBlank correlationId
│   ├── BaseResDTO        — abstract @Data class with response fields
│   ├── IBaseService      — @Validated interface: B execute(@Valid A req)  (synchronous)
│   ├── enums/ResponseEnum — S00_SUCCESS(HttpStatus.OK, "00", "Success")
│   └── aspect/ExecuteTraceAspect — @Around IBaseService.execute() → injects traceId/timestamp
└── uam/                 — Spring Boot app (com.anasdidi.uam.*)
    ├── UamApplication    — entrypoint, scanBasePackages="com.anasdidi"
    ├── controller/       — interface-first: interface declares @GetMapping, impl is @RestController
    ├── service/          — UamService extends IBaseService, impls are @Service @Validated
    └── dto/              — extends abstract BaseReqDTO/BaseResDTO
```

**Module dependency**: `common` → `uam` (one-way). A change in `common` needs `./mvnw compile -pl uam -am` to rebuild consumers.

---

## Architecture notes

- **Stack is servlet (not reactive)**: uses `spring-boot-starter-web` and `spring-boot-starter-data-jpa`. Services are synchronous (`B execute(...)`, not `Mono<B>`).
- **Interface-first controllers**: interface class declares `@GetMapping` etc.; `@RestController` impl class adds `@RequestMapping` with the full path.
- **Generic service layer**: `UamService<A extends BaseReqDTO, B extends BaseResDTO>` → `IBaseService<A, B>` → single `B execute(@Valid A req)`.
- **ExecuteTraceAspect** intercepts all `IBaseService.execute()` calls via `@Around` advice. It sets MDC traceId/spanId, and on return populates `traceId`, `timestamp`, `timeTaken`, `responseCode`/`responseDesc` in the `BaseResDTO`. Impl services only need to set `ResponseEnum`; the aspect fills in the rest.
- API path: `/uam` (`CommonConstants.CONTEXT_PATH`) + `/v1` + endpoint path.
- Current endpoint: `GET /uam/v1/hello-world/greeting?name=...` (requires `App-Correlation-Id` header).

---

## DTO conventions

- **Request DTOs**: Lombok `@Data` + `@SuperBuilder` + `@Jacksonized`, extend `BaseReqDTO` (carries `@NotBlank correlationId`). Use a **nested payload** pattern: an inner `@Data` `@SuperBuilder` static class annotated `@Valid @NonNull`.
- **Response DTOs**: Lombok `@Data` + `@SuperBuilder` + `@Jacksonized`, extend `BaseResDTO`. Carry a `@JsonIgnore ResponseEnum response` field (only code/message appear in JSON payload, populated by `ExecuteTraceAspect`).
- Correlation ID flows via the `App-Correlation-Id` header → controller builds the req DTO with it.

---

## Testing

- **Controller tests**: `@ExtendWith(MockitoExtension.class)` + `MockMvcBuilders.standaloneSetup()` (pure Mockito, no Spring slice loading).
- **Service tests**: `@SpringBootTest` (full context). Services are synchronous — call directly, no `.block()`.
- **`common` module**: no `src/test/` directory — no tests exist yet.
- **Liquibase**: `db/changelog/db.changelog-master.xml` exists but is **empty** (no `<changeSet>` entries). No DB migrations configured yet.
- **Security**: `spring-boot-starter-security` (and test counterpart) are **commented out** in `uam/pom.xml` — app is fully unauthenticated.
- **H2 console** (`spring-boot-h2console`) and **Actuator** (unconfigured) are included for development.
- No test-specific resources directory.

---

## Code style

- **Spotless** (parent POM): `palantirJavaFormat` (style=GOOGLE, 2-space indent), `removeUnusedImports`, `formatAnnotations`. POM sorted with 2-space indent via `sortPom`. XML resources via Eclipse WTP.
- **EditorConfig**: 2-space indent for Java/XML, LF line endings.
- Do not manually format — use `./mvnw spotless:apply`.

---

## Environment

- Dev container via Docker Compose (`mcr.microsoft.com/devcontainers/java:3-25-trixie`). OpenCode pre-installed via devcontainer feature.
- No `.env` file — launch config may reference one but not wired.
- Logback writes to `uam/logs/` (file appender), gitignored via root `.gitignore`).
- Spring Boot JVM args: `--enable-native-access=ALL-UNNAMED` (set in parent POM).
- H2 in-memory DB (`jdbc:h2:mem:uamdb`), JPA ddl-auto=update.

---

## Git

- Conventional commits: `feat:`, `fix:`, `refactor:`, `build:`, `chore:`, etc.
- Flow: `develop` ← `feature/*`. No CI workflows exist.

---

## Module-specific AGENTS.md

- [`app/AGENTS.md`](app/AGENTS.md) — command instructions (run from `app/`)
- [`app/common/AGENTS.md`](app/common/AGENTS.md) — common library JAR notes
- [`app/uam/AGENTS.md`](app/uam/AGENTS.md) — uam application notes
