# AGENTS.md — `app/common`

See the root [`/home/vscode/workspace/AGENTS.md`](../../AGENTS.md) for full repo guidance.

## Module-specific notes

- **Library JAR** (no main class, not runnable). Consumed by `uam`.
- All classes live under `com.anasdidi.common.*`.
- Dependencies: `spring-boot-starter-webflux`, `spring-boot-starter-aspectj`, `springdoc-openapi-starter-webflux-ui`, Lombok (optional).
- A change here requires rebuilding `uam` to pick it up: `./mvnw compile -pl uam -am`.
- **No tests exist yet** — `src/test/` directory does not exist; any new code must be tested from scratch.
