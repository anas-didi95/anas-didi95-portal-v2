# AGENTS.md — `app/uam` (User Account Management)

## Project identity

- **Repo**: `anas-didi95/anas-didi95-portal-v2` — monorepo; multi-module Maven project under `app/`.
- **Modules**: `common` (library JAR) → `uam` (Spring Boot application).
- **What uam is**: Spring Boot 4.0.6 / WebFlux / R2DBC + H2 / Liquibase reactive API service.
- **Java 25**, Maven wrapper (`./mvnw` at `app/` level), Lombok, SpringDoc OpenAPI.
- Current branch: `feature/user-service`. Develop branch: `develop`.
- No `.env` file — launch config references `${workspaceFolder}/.env` but it does not exist yet.

## Key commands (run from `app/`)

| Command | Purpose |
|---|---|
| `./mvnw compile` | Compile all modules |
| `./mvnw compile -pl uam -am` | Compile uam + its deps (common) |
| `./mvnw compile -pl common` | Compile only common |
| `./mvnw test -pl uam -am` | Run all uam tests |
| `./mvnw spotless:check` | Check formatting on all modules |
| `./mvnw spotless:check -pl uam` | Check formatting on uam only |
| `./mvnw spotless:apply` | Auto-format all modules |
| `./mvnw verify` | Full pipeline: tests + spotless check (all modules) |
| `./mvnw verify -pl uam -am -DskipTests` | Format-check only uam (skip tests) |
| `./mvnw spring-boot:run -pl uam` | Start the app |
| `./mvnw clean verify` | Full clean build with tests + formatting |

**Order matters**: Spotless `check` is bound to the `verify` phase. To format + test, run `spotless:apply` first, then `verify`.

## Architecture

```
app/
├── pom.xml              — Parent POM (modules, dependency mgmt, Spotless)
├── common/              — Library JAR (com.anasdidi.uam.common.*)
│   └── src/main/java/com/anasdidi/uam/common/
│       ├── CommonConstants.java
│       ├── IBaseReqDTO.java
│       ├── IBaseResDTO.java
│       └── enums/ResponseEnum.java
└── uam/                 — Spring Boot application
    └── src/main/java/com/anasdidi/uam/
        ├── controller/  — Interface + versioned impls
        ├── service/     — Interface + impl (generic: UamService<ReqDTO, ResDTO>)
        └── dto/         — Java records with Lombok @Builder, nested Payload records
```

- All controllers follow an **interface-first** pattern: the interface declares `@RequestMapping` / `@GetMapping` etc., the impl class implements it with `@RestController`.
- The generic `UamService<A, B>` has a single `Mono<B> execute(@Valid A req)` method — every service follows this.
- API base path: `/uam` (via `CommonConstants.CONTEXT_PATH`).
- API version segment: `/v1`.
- Current endpoints: `/uam/v1/hello-world/greeting`.

## Request/Response conventions

- All request DTOs implement `IBaseReqDTO` (marker interface).
- All response DTOs implement `IBaseResDTO` (marker interface) and carry a `ResponseEnum response` field.
- Correlation ID is passed via the `App-Correlation-Id` header and threaded through the service.
- Validation uses `jakarta.validation` annotations (`@NotBlank`, `@NotNull`, `@Valid`). Services are `@Validated`.
- Responses use `ResponseEnum` with `HttpStatus` + code + message.

## Code style & formatting

- **Spotless** defined in **parent POM** (`app/pom.xml`), inherited by all modules.
- Rules: **palantirJavaFormat** (style=GOOGLE), **removeUnusedImports**, **formatAnnotations**.
- POM is sorted with 2-space indent via `sortPom`.
- XML resources formatted via Eclipse WTP.
- EditorConfig: 2-space indent for Java, XML; LF line endings; trailing whitespace trimmed.
- Formatter run via `./mvnw spotless:apply` from `app/` — **do not manually format**.

## Testing quirks

- **Controller tests**: `@WebFluxTest` + `WebTestClient`. Mock the service via a `@TestConfiguration` inner class returning `Mockito.mock()`.
- **Service tests**: `@SpringBootTest` (full context). Use `.block()` on reactive `Mono` return values.
- No embedded DB or R2DBC test slices yet — Liquibase changelogs directory is empty, so there's no DB migration to run.

## DB / Liquibase

- R2DBC + H2 (in-memory, runtime scope).
- Liquibase starter is included; changelog directory (`src/main/resources/db/changelog/`) is **empty**.
- To add migrations, create changelog files there — no existing patterns to follow yet.

## Stalled or commented-out features

- `spring-boot-starter-security` (and its test counterpart) are **commented out** in `pom.xml`.
- No security configuration exists — the app is fully unauthenticated.
- H2 console is included (`spring-boot-h2console`).

## Dev environment

- Dev container (Docker Compose) with Java 25 image. Runs as `vscode` inside container.
- OpenCode is pre-installed in the dev container.
- VS Code extensions: Boot Dev Pack, Tailwind CSS Kit, Liquibase Snippets.
- Root `.gitignore` ignores `logs/`, `.spotless-cache/`, and `*.log`.

## Git conventions

- Conventional commits: `feat:`, `fix:`, `refactor:`, `build:`, `chore:`, etc.
- Branch flow: `develop` → `feature/*` → merge back to `develop`.
- Current feature branch: `feature/user-service`.
