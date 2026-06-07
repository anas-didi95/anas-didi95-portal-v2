# AGENTS.md — `app/uam`

See the root [`/home/vscode/workspace/AGENTS.md`](../../AGENTS.md) for full repo guidance.

## Module-specific notes

- **Spring Boot application** module. Depends on `common` (`com.anasdidi.common.*`).
- **Entrypoint**: `com.anasdidi.uam.UamApplication` with `scanBasePackages = "com.anasdidi"` (picks up `common` aspects).
- Controllers, services, and DTOs live under `com.anasdidi.uam.*`.
- **Do not use** `app/uam/mvnw` — the canonical wrapper is `app/mvnw`.
- Liquibase changelogs dir (`src/main/resources/db/changelog/`) is **empty** — no DB migrations exist yet.
- Logback writes to `./logs/` (gitignored via root `.gitignore`).
