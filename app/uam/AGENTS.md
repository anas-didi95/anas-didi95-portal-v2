# AGENTS.md — `app/uam`

See the root [`/home/vscode/workspace/AGENTS.md`](../../AGENTS.md) for full repo guidance.

## Module-specific notes

- **Spring Boot application** module. Depends on `common` (`com.anasdidi.common.*`) — its aspects and base classes.
- **Entrypoint**: `com.anasdidi.uam.UamApplication` with `scanBasePackages = "com.anasdidi"` (picks up `common` components).
- Controllers, services, and DTOs live under `com.anasdidi.uam.*`.
- **Do not use** `app/uam/mvnw` — the canonical wrapper is `app/mvnw`.
- **Liquibase**: Master changelog at `src/main/resources/db/db.changelog-master.xml`. Currently includes one `T_USER` migration.
- **Logback**: Writes to `./logs/uam.log` (relative to `app/uam/`), gitignored via root `.gitignore`.
- **Test config**: `src/test/resources/application.properties` activates `test` profile → in-memory H2 (`jdbc:h2:mem:uamdb`).
