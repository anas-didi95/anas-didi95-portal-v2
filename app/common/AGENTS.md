# AGENTS.md — `app/common`

See [root AGENTS.md](../../AGENTS.md) for full repo guidance.

- **Library JAR** (no main class, not runnable). Consumed by `uam`.
- Package: `com.anasdidi.common.*`.
- Dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-aspectj`, `springdoc-openapi-starter-webmvc-ui`, Lombok (optional).
- Changing `common` requires rebuilding `uam`: `./mvnw compile -pl uam -am`.
- **No tests exist** — `src/test/` directory absent; any new tests must be created from scratch.
