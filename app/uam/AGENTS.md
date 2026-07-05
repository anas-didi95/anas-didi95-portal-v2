# AGENTS.md — `app/uam`

See [root AGENTS.md](../../AGENTS.md) for full repo guidance.

- **Spring Boot application** module. Depends on `common` for aspects, base classes, and config.
- **Entrypoint**: `com.anasdidi.uam.UamApplication` with `scanBasePackages = "com.anasdidi"`.
- Package: `com.anasdidi.uam.*` (controllers, services, DTOs, entities, repositories).
- **Do not use** `app/uam/mvnw` — the canonical wrapper is `app/mvnw`.
- **Liquibase**: Master changelog at `src/main/resources/db/db.changelog-master.xml`.
- **Logback**: Writes to `./logs/uam.log` (relative to `app/uam/`), archived daily with rolling policy. Gitignored via root `.gitignore`.
- **Test config**: `src/test/resources/application.properties` activates `test` profile → in-memory H2 (`jdbc:h2:mem:uamdb`).
