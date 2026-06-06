# AGENTS.md — `app/uam` (User Account Management)

## Project identity

- **Repo**: `anas-didi95/anas-didi95-portal-v2` — monorepo; `app/uam` is the only sub-project today.
- **What it is**: Spring Boot 4.0.6 / WebFlux / R2DBC + H2 / Liquibase reactive API service.
- **Java 25**, Maven wrapper (`./mvnw`), Lombok, SpringDoc OpenAPI.
- Current branch: `feature/user-service`. Develop branch: `develop`.
- No `.env` file — launch config references `${workspaceFolder}/.env` but it does not exist yet.

## Key commands (run from `app/uam/`)

| Command | Purpose |
|---|---|
| `./mvnw compile` | Compile only |
| `./mvnw test` | Run all tests |
| `./mvnw spotless:check` | Check formatting without compiling |
| `./mvnw spotless:apply` | Auto-format all Java / XML / POM files |
| `./mvnw verify -DskipTests` | Format-check only (skip tests) |
| `./mvnw verify` | Full pipeline: tests + spotless check |
| `./mvnw spring-boot:run` | Start the app |

**Order matters**: Spotless `check` is bound to the `verify` phase. To format + test, run `spotless:apply` first, then `verify`.

## Architecture

```
controller/          — Interface + versioned impls (e.g. HelloWorldController / HelloWorldControllerV1)
service/             — Interface + impl (generic: UamService<ReqDTO, ResDTO>)
dto/                 — Java records with Lombok @Builder, nested Payload records
common/              — Constants, base interfaces (IBaseReqDTO, IBaseResDTO), enums (ResponseEnum)
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

- **Spotless** with **palantirJavaFormat** (style=GOOGLE), **removeUnusedImports**, **formatAnnotations**.
- POM is sorted with 2-space indent via `sortPom`.
- XML resources formatted via Eclipse WTP.
- EditorConfig: 2-space indent for Java, XML; LF line endings; trailing whitespace trimmed.
- Formatter run via `./mvnw spotless:apply` — **do not manually format**.

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
