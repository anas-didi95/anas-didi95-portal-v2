# AGENTS.md — `app/common`

See the root [`/home/vscode/workspace/AGENTS.md`](../../AGENTS.md) for full repo guidance.

## Module-specific notes

- This is a **library JAR** (no main class, not runnable). It is consumed by `uam`.
- All classes live under `com.anasdidi.common.*`.
- Depends only on `spring-web` + Lombok (both marked optional/compile-only; no transitive impact on consumers).
- A change here requires rebuilding `uam` to pick it up: `./mvnw compile -pl uam -am`.
- **No tests exist yet** — `src/test/` directory does not exist; any new code in common must be tested from scratch.
