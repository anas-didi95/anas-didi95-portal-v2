# AGENTS.md — `app/` (Maven project root)

This is the working directory for all commands. See the root [`/home/vscode/workspace/AGENTS.md`](../AGENTS.md) for full repo guidance.

## Canonical `mvnw`

The Maven wrapper at `app/mvnw` is the only valid one. There is a stale duplicate at `app/uam/mvnw` — do not use it.

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

**Order matters**: Spotless `check` runs during `verify`. Run `spotless:apply` first, then `verify`.

## Notable POM quirks

- `spring-boot-starter-security` (and test counterpart) are **commented out** — app is fully unauthenticated.
- H2 console (`spring-boot-h2console`) is included for development.
- Actuator (`spring-boot-starter-actuator`) is included but unconfigured.
- Logback writes to `uam/logs/` (gitignored via root `.gitignore`).
- **JaCoCo** enforces ≥80% line coverage for `com.anasdidi.uam.controller.impl` and `com.anasdidi.uam.service.impl` during `verify`.
