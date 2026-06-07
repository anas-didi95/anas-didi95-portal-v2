# AGENTS.md — `app/uam`

See the root [`/home/vscode/workspace/AGENTS.md`](../../AGENTS.md) for full repo guidance.

## Module-specific notes

- This is the Spring Boot application module. It depends on `common` (`com.anasdidi.common.*`).
- Controllers, services, and DTOs live under `com.anasdidi.uam.*`.
- **Current entrypoint**: `com.anasdidi.uam.UamApplication`.
- **Do not use** `app/uam/mvnw` — the canonical wrapper is at `app/mvnw`.
- Liquibase changelogs dir (`src/main/resources/db/changelog/`) is **empty**.
- Logback writes to `./logs/` (gitignored).
