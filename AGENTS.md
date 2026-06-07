# AGENTS.md

## What this is

Multi-module Maven project under `app/`: **common** (library JAR) → **uam** (Spring Boot application).  
All developer commands run from `app/` — see [`app/AGENTS.md`](app/AGENTS.md) for the full command reference.

**Stack**: Java 25, Spring Boot 4.0.6 / WebFlux / R2DBC + H2 / Liquibase, Lombok, SpringDoc OpenAPI.  
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

**The canonical `mvnw` is at `app/mvnw`**. There is a stale duplicate at `app/uam/mvnw` — do not use it.

**Order matters**: Spotless `check` is bound to `verify` phase. For format + test: run `spotless:apply` first, then `verify`.

**No CI workflows exist** — no `.github/workflows/` directory.

---

## Architecture

```
app/
├── pom.xml              — Parent POM (modules, Spotless config)
├── common/              — Library JAR (com.anasdidi.common.*)
└── uam/                 — Spring Boot app (com.anasdidi.uam.*)
```

- **Interface-first controllers**: interface declares `@RequestMapping`/`@GetMapping` etc., impl class is `@RestController`.
- **Generic service**: `UamService<A extends IBaseReqDTO, B extends IBaseResDTO>` with single `Mono<B> execute(@Valid A req)`. The interface is `@Validated` for method-level validation.
- API base path: `/uam` (`CommonConstants.CONTEXT_PATH`), version segment: `/v1`.
- Current endpoint: `GET /uam/v1/hello-world/greeting?name=...` (requires `App-Correlation-Id` header).

---

## DTO conventions

- Request DTOs = Java records with `@Builder`, implement `IBaseReqDTO`, use `@NotBlank`/`@NotNull`/`@Valid`.
- Response DTOs = Java records with `@Builder`, implement `IBaseResDTO`, carry a `ResponseEnum response` field (`@JsonIgnore` so only code/message appear in payload).
- Request DTOs use a **nested payload record** pattern: the outer record holds `correlationId` + a `@Valid @NotNull` inner payload record.
- Correlation ID threaded via `App-Correlation-Id` header.

---

## Code style

- **Spotless** in parent POM: palantirJavaFormat (style=GOOGLE), removeUnusedImports, formatAnnotations.
- POM sorted with 2-space indent, XML resources via Eclipse WTP.
- EditorConfig: 2-space indent for Java/XML, LF line endings.
- Do not manually format — use `./mvnw spotless:apply`.

---

## Testing

- **Controller tests**: `@WebFluxTest` + `WebTestClient` + `@TestConfiguration` returning `Mockito.mock()` for the service.
- **Service tests**: `@SpringBootTest` (full context). Use `.block()` on reactive `Mono` returns.
- Liquibase changelogs dir is **empty** — no DB migrations exist yet, so no test slices depend on them.
- `spring-boot-starter-security` (and test counterpart) are **commented out** in `pom.xml` — the app is fully unauthenticated.
- H2 console (`spring-boot-h2console`) is included for development.
- Actuator (`spring-boot-starter-actuator`) is included but unconfigured.

---

## Environment

- Dev container (Docker Compose, `mcr.microsoft.com/devcontainers/java:3-25-trixie`).
- No `.env` file exists — launch config may reference one but it's not wired in.
- OpenCode is pre-installed in the dev container.
- Logback writes to `app/uam/logs/` (gitignored via root `.gitignore`).
- Root `.gitignore` ignores `logs/`, `.spotless-cache/`, `*.log`.

---

## Git

- Conventional commits: `feat:`, `fix:`, `refactor:`, `build:`, `chore:`, etc.
- Flow: `develop` ← `feature/*`.
