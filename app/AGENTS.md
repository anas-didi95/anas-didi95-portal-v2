# AGENTS.md — `app/` (Maven project root)

The working directory for all Maven commands. See [root AGENTS.md](../AGENTS.md) for full repo guidance.

## Canonical `mvnw`

Only `app/mvnw` is valid. The stale duplicate at `app/uam/mvnw` must not be used.

## POM quirks

- `spring-boot-starter-security` (and test counterpart) are **commented out** — app is fully unauthenticated.
- H2 console and Actuator (both unconfigured) are included for development.
- **JaCoCo** enforces ≥80% line coverage for `com.anasdidi.uam.controller.impl` and `com.anasdidi.uam.service.impl` during `verify`.
- Logback writes to `app/uam/logs/uam.log` (gitignored via root `.gitignore`).
- Prod datasource configured via env vars (`UAM_DB_USERNAME`, `UAM_DB_PASSWORD`, `UAM_DB_URL`).
