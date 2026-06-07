# AGENTS.md — `app/` (Maven project root)

This is the working directory for all commands. The `mvnw` wrapper here is the canonical one.

---

## Multi-module layout

```
app/
├── pom.xml          — Parent POM (modules, dependency mgmt, Spotless config)
├── common/          — Library JAR  →  com.anasdidi.common.*
└── uam/             — Spring Boot  →  com.anasdidi.uam.*
```

**Module dependency**: `common` → `uam` (no reverse dependency).

---

## Commands (run from here)

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

**Do not use** `uam/mvnw` — there are two `mvnw` files and only this directory's version is canonical.

**Order matters**: Spotless `check` runs during `verify`. To format + test: run `spotless:apply` first, then `verify`.

**No CI workflows** exist anywhere in the repo.

---

## Architecture

- **Interface-first controllers**: interface declares `@RequestMapping`/`@GetMapping`, impl is `@RestController`.
- **Generic service**: `UamService<A extends IBaseReqDTO, B extends IBaseResDTO>` with single `Mono<B> execute(@Valid A req)`. Interface is `@Validated` for method-level validation.
- API base path: `/uam` (`CommonConstants.CONTEXT_PATH`), version segment: `/v1`.
- Current endpoint: `GET /uam/v1/hello-world/greeting?name=<name>` (requires `App-Correlation-Id` header).

---

## DTO conventions

- **Request**: Java record with `@Builder`, implements `IBaseReqDTO`. Uses a **nested payload record** — the outer record holds `correlationId` + a `@Valid @NotNull` inner record.
- **Response**: Java record with `@Builder`, implements `IBaseResDTO`. Carries `ResponseEnum response` with `@JsonIgnore` (only code/message appear in JSON).
- Correlation ID threaded via `App-Correlation-Id` header.

---

## Code style

- **Spotless** in parent POM: palantirJavaFormat (style=GOOGLE), removeUnusedImports, formatAnnotations. POM sorted with 2-space indent via sortPom.
- **EditorConfig**: 2-space indent for Java/XML, LF line endings.
- Do not manually format — run `./mvnw spotless:apply`.

---

## Testing

- **Controller tests**: `@WebFluxTest` + `WebTestClient` + `@TestConfiguration` returning `Mockito.mock()` for the service.
- **Service tests**: `@SpringBootTest` (full context). Use `.block()` on reactive `Mono` returns.
- `common` module has **no tests** (no `src/test/` directory).
- **No DB migrations** exist yet — Liquibase changelogs dir is empty. No test slices depend on them.

---

## Notable POM quirks

- `spring-boot-starter-security` (and test counterpart) are **commented out** — app is fully unauthenticated.
- H2 console (`spring-boot-h2console`) is included for development.
- Actuator (`spring-boot-starter-actuator`) is included but unconfigured.
- Logback writes to `uam/logs/` (gitignored).
