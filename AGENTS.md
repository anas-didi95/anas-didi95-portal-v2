# AGENTS.md

Multi-module Maven project under `app/`: **common** (library JAR) → **uam** (Spring Boot application).

**Stack**: Java 25, Spring Boot 4.0.6 / WebFlux / R2DBC + H2 / Liquibase, Lombok, SpringDoc OpenAPI.  
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

**Order matters**: Spotless `check` is bound to `verify` phase. For format + test: run `spotless:apply` first, then `verify`.

---

## Project layout

```
app/
├── pom.xml              — Parent POM (modules, Spotless, dep mgmt)
├── common/              — Library JAR (com.anasdidi.common.*)
│   ├── CommonConstants   — CONTEXT_PATH=/uam, API_V1=/v1, HEADER_CORR_ID
│   ├── BaseReqDTO        — abstract @Data class with @NotBlank correlationId
│   ├── BaseResDTO        — abstract @Data class with response fields
│   ├── IBaseService      — @Validated interface: Mono<B> execute(@Valid A req)
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

## DTO conventions

- **Request DTOs**: Lombok `@Data` + `@SuperBuilder`, extend `BaseReqDTO` (which carries `@NotBlank correlationId`). Use a **nested payload** pattern: an inner `@Data` `@SuperBuilder` static class annotated `@Valid @NonNull`.
- **Response DTOs**: Lombok `@Data` + `@SuperBuilder`, extend `BaseResDTO`. Carry a `@JsonIgnore ResponseEnum response` field (only code/message appear in the JSON payload, populated by `ExecuteTraceAspect`).
- Correlation ID flows via the `App-Correlation-Id` header → controller builds the req DTO with it.

---

## Architecture notes

- **Interface-first controllers**: interface class declares `@GetMapping` etc.; `@RestController` impl class adds `@RequestMapping` with the full path.
- **Generic service layer**: `UamService<A extends BaseReqDTO, B extends BaseResDTO>` → `IBaseService<A, B>` → single `Mono<B> execute(@Valid A req)`.
- **ExecuteTraceAspect** intercepts all `IBaseService.execute()` calls via `@Around` advice. It sets MDC traceId/spanId, and on response completion populates `traceId`, `timestamp`, `timeTaken`, `responseCode`/`responseDesc` in the `BaseResDTO`. This means impl services only set `ResponseEnum`; the aspect fills in the rest.
- API path: `/uam` (`CommonConstants.CONTEXT_PATH`) + `/v1` + endpoint path.
- Current endpoint: `GET /uam/v1/hello-world/greeting?name=...` (requires `App-Correlation-Id` header).

---

## Code style

- **Spotless** (parent POM): palantirJavaFormat (style=GOOGLE), removeUnusedImports, formatAnnotations. POM sorted with 2-space indent via sortPom. XML resources via Eclipse WTP.
- **EditorConfig**: 2-space indent for Java/XML, LF line endings.
- Do not manually format — use `./mvnw spotless:apply`.

---

## Testing

- **Controller tests**: `@WebFluxTest` + `WebTestClient` + `@TestConfiguration` returning `Mockito.mock()` for the service.
- **Service tests**: `@SpringBootTest` (full context). Call `.block()` on reactive `Mono` returns for assertions.
- **`common` module**: no tests exist yet (no `src/test/` directory).
- **Liquibase**: changelogs dir is **empty** — no DB migrations exist. No test slices depend on them.
- **Security**: `spring-boot-starter-security` (and test counterpart) are **commented out** in `uam/pom.xml` — fully unauthenticated.
- **H2 console** (`spring-boot-h2console`) and **Actuator** (unconfigured) are included for development.

---

## Environment

- Dev container via Docker Compose (`mcr.microsoft.com/devcontainers/java:3-25-trixie`). OpenCode pre-installed.
- No `.env` file — launch config may reference one but not wired.
- Logback writes to `app/uam/logs/` (gitignored via root `.gitignore`).
- Spring Boot JVM args: `--enable-native-access=ALL-UNNAMED` (set in parent POM).

---

## Git

- Conventional commits: `feat:`, `fix:`, `refactor:`, `build:`, `chore:`, etc.
- Flow: `develop` ← `feature/*`. No CI workflows exist.

---

## Module-specific AGENTS.md

- [`app/common/AGENTS.md`](app/common/AGENTS.md) — common library JAR notes
- [`app/uam/AGENTS.md`](app/uam/AGENTS.md) — uam application notes
