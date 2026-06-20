# Plan: Add YAML Formatter to Spotless

## Objective

Add a YAML formatting step to the existing Spotless Maven plugin configuration in `app/pom.xml`, using the built-in Jackson YAML formatter, so that all `*.yml` and `*.yaml` files under the Maven project (`app/`) are automatically checked and formatted during the build.

## Requirements Snapshot

- **R1:** Spotless (v2.44.3) must format all `*.yml` and `*.yaml` files under `app/` using the Jackson YAMLFactory.
- **R2:** The YAML formatter must use 2-space indentation (YAML standard) and preserve LF line endings.
- **R3:** Running `./mvnw spotless:apply` must format the YAML files.
- **R4:** Running `./mvnw verify` (or `spotless:check`) must validate YAML formatting and fail the build if violations exist.
- **R5:** The existing Java, POM, and XML formatting must continue to work unchanged.

## Scope

- Add `<yaml>` configuration block to the Spotless plugin in `app/pom.xml`.
- Add `<includes>` pattern targeting `src/**/*.yml` and `src/**/*.yaml` within the Maven project.
- Run `./mvnw spotless:apply` to verify formatting works.
- Run `./mvnw verify` to verify the full pipeline passes.
- **Not in scope:** The root `docker-compose.yml` (outside the Maven project, not covered by Maven Spotless execution).

## Assumptions and Constraints

- Spotless v2.44.3 ships with a built-in `<yaml>` step using Jackson's `jackson-dataformat-yaml`.
- The YAML step is a top-level element (like `<java>` and `<pom>`), not a `<format>` inside `<formats>`.
- Target files must be explicitly listed via `<includes>` (YAML is not auto-discovered like Java).
- The Maven project root is `app/`, so includes are relative to that directory.
- No `.yaml` files currently exist in the project — only `.yml` files — but support for both is included for future-proofing.
- The project's global `<lineEndings>` policy is not currently set; we should add `LF` to be explicit (matching the existing style).

## Risks and Areas Requiring Care

1. **Jackson version mismatch:** The bundled Jackson version in Spotless must be compatible. Spotless 2.44.3 bundles its own Jackson — no external dependency needed.
2. **YAML formatting changes may alter content:** Jackson's YAML pretty-printer may change quoting (e.g., unquoted strings to quoted, or vice versa), which can affect Spring Boot property loading. Spotless's Jackson mode uses defaults that preserve safe quoting.
3. **Root `docker-compose.yml` not covered:** Because Spotless runs from the Maven POM (`app/`), files outside the Maven project root are not included. The root `docker-compose.yml` would need a separate formatter (e.g., Prettier or manual formatting if desired).
4. **Existing files may need reformatting:** Running `spotless:apply` for the first time may reformat existing YAML files, changing whitespace or quoting. This is expected and should produce clean, consistent output.

## Core Concepts

### Spotless YAML step

The YAML step uses Jackson's `YAMLFactory` to parse and pretty-print YAML content. It is a top-level configuration block alongside `<java>` and `<pom>`:

```xml
<yaml>
  <includes>
    <include>src/**/*.yml</include>
    <include>src/**/*.yaml</include>
  </includes>
  <jackson>
    <!-- Optional Jackson SerializationFeature settings -->
    <features>
      <INDENT_OUTPUT>true</INDENT_OUTPUT>
    </features>
    <!-- Optional YAMLGenerator.Feature settings -->
    <yamlFeatures>
      <MINIMIZE_QUOTES>false</MINIMIZE_QUOTES>
    </yamlFeatures>
  </jackson>
</yaml>
```

### Jackson YAML formatting behavior

| Concern | Default | Notes |
|---|---|---|
| Indentation | 2 spaces | YAML standard, matches project style |
| Quote style | Minimal quoting | `MINIMIZE_QUOTES=false` preserves existing quoting |
| Document start marker | Not written | `WRITE_DOC_START_MARKER=false` (default) |
| Line endings | Inherits from global `lineEndings` setting | Should set `LF` at the configuration level |

### How it integrates with the existing pipeline

```text
mvn spotless:check   → checks Java + POM + XML + YAML
mvn spotless:apply   → formats Java + POM + XML + YAML
mvn verify / deploy  → binds spotless:check for all formats
```

## Sub-Tasks

### Sub-Task 1: Add YAML formatter configuration to `app/pom.xml`

- **Status:** Pending
- **Objective:** Add the `<yaml>` configuration block to the Spotless plugin in `app/pom.xml`, targeting all `.yml` and `.yaml` files under the Maven project.
- **Related Requirements:** R1, R2, R5
- **Dependencies and Preconditions:** None
- **In Scope for This Sub-Task:**
  - Edit `app/pom.xml` to add a `<yaml>` block under `<configuration>` in the `spotless-maven-plugin`.
  - Include patterns for `src/**/*.yml` and `src/**/*.yaml`.
  - Configure Jackson formatter with sensible defaults (2-space indent, LF line endings via global `<lineEndings>`).
  - Preserve all existing `<java>`, `<pom>`, and `<formats>` blocks exactly as they are.
- **Out of Scope for This Sub-Task:**
  - Adding the root `docker-compose.yml` — it's outside the Maven project.
  - Adding Prettier as an alternative YAML formatter.
  - Adding `*.yaml` support to the existing XML `<formats>` block.
- **Instructions:**
  1. Read `app/pom.xml`.
  2. Inside the `<configuration>` element of the `spotless-maven-plugin` (after the closing `</formats>` tag), add a `<lineEndings>` element set to `LF` at the configuration level (if not already present), then add the `<yaml>` block.
  3. The `<yaml>` block should contain:
     ```xml
     <yaml>
       <includes>
         <include>src/**/*.yml</include>
         <include>src/**/*.yaml</include>
       </includes>
       <jackson>
         <features>
           <INDENT_OUTPUT>true</INDENT_OUTPUT>
         </features>
         <yamlFeatures>
           <MINIMIZE_QUOTES>false</MINIMIZE_QUOTES>
         </yamlFeatures>
       </jackson>
     </yaml>
     ```
  4. Add `<lineEndings>LF</lineEndings>` as a top-level element inside `<configuration>` (before `<java>`) to ensure all formats use LF consistently. If it already exists, skip this step.
- **Acceptance Criteria:**
  - `app/pom.xml` contains a valid `<yaml>` block with proper includes.
  - Existing `<java>`, `<pom>`, and `<formats>` blocks are unchanged.
  - `./mvnw compile` succeeds (POM is well-formed).
- **Cautionary Points (Risks & Edge Cases):**
  - The `<yaml>` element must come at the end of `<configuration>`, or at least after `<formats>`, though order generally doesn't matter for Spotless.
  - XML must be well-formed — remember to close tags properly.
  - The `<includes>` element in `<yaml>` uses the same structure as `<format>` includes but is a direct child of `<yaml>`, NOT wrapped in `<target>`.
- **Implementation Suggestions:**
  - The `<lineEndings>` element goes at the configuration level (child of `<configuration>`) and applies to ALL format steps.
  - Jackson version is not specified, so Spotless uses its bundled version. Add `<version>` only if a specific Jackson version is needed (not needed here).
- **Testing Suggestions:**
  - `./mvnw compile` — basic POM validity check.
  - `./mvnw spotless:check -pl uam -am` — should run without errors (YAML check should pass if files are already clean).
- **Done When:** `./mvnw compile` succeeds and the YAML configuration is present in `app/pom.xml`.

---

### Sub-Task 2: Run `spotless:apply` and verify YAML formatting

- **Status:** Pending
- **Objective:** Run the Spotless apply goal and verify that all YAML files are formatted correctly. Inspect any changes made to existing YAML files.
- **Related Requirements:** R2, R3
- **Dependencies and Preconditions:** Sub-Task 1 completed.
- **In Scope for This Sub-Task:**
  - Run `./mvnw spotless:apply` to format all files (Java, POM, XML, YAML).
  - Check `git diff` to see what changed in the YAML files.
  - Ensure the changes are minimal and correct (no broken YAML content, proper formatting).
- **Out of Scope for This Sub-Task:**
  - Committing changes (user commits when ready).
  - Running full `verify` pipeline (handled in Sub-Task 3).
- **Instructions:**
  1. Run `./mvnw spotless:apply` from `app/`.
  2. Run `git diff` to inspect formatting changes to YAML files.
  3. Verify the YAML content is still valid (Spring Boot can parse it).
- **Acceptance Criteria:**
  - `spotless:apply` completes without errors.
  - YAML files are consistently formatted (2-space indent, LF line endings).
  - The existing YAML content is semantically preserved (no keys removed, no values changed).
- **Cautionary Points (Risks & Edge Cases):**
  - Jackson may change quoting on string values (e.g., `"sa"` → `sa` or vice versa). Double-check that Spring Boot's YAML parser handles the output correctly.
  - If the Jackson formatter removes quotes that Spring Boot needs (e.g., around URLs with `:`), this could break configuration. Verify with a compile/test run.
- **Implementation Suggestions:**
  - If Jackson's default quoting is problematic, consider enabling `MINIMIZE_QUOTES` feature or adding explicit `<yamlFeatures>` to preserve more quoting.
- **Testing Suggestions:**
  - `./mvnw spotless:apply`
  - `git diff` to verify changes.
  - `./mvnw compile -pl uam -am` to confirm the YAML is still valid config.
- **Done When:** `spotless:apply` succeeds, YAML files are consistently formatted, and the project still compiles.

---

### Sub-Task 3: Run full `verify` pipeline

- **Status:** Pending
- **Objective:** Run the full Maven verify pipeline to confirm all formatting checks (including YAML) pass, tests pass, and the build succeeds end-to-end.
- **Related Requirements:** R4, R5
- **Dependencies and Preconditions:** Sub-Task 2 completed (YAML files are already formatted).
- **In Scope for This Sub-Task:**
  - Run `./mvnw verify` from `app/`.
  - Confirm Spotless `check` passes for all formats (Java, POM, XML, YAML).
  - Confirm tests still pass (no regressions from YAML formatting).
  - Confirm the build succeeds with no errors.
- **Out of Scope for This Sub-Task:**
  - JaCoCo coverage or other checks not related to formatting.
- **Instructions:**
  1. Run `./mvnw verify` from `app/`.
  2. Check the build output for any Spotless failures.
- **Acceptance Criteria:**
  - `./mvnw verify` succeeds.
  - No Spotless failures (Java, POM, XML, or YAML).
  - All tests pass.
- **Cautionary Points (Risks & Edge Cases):**
  - If `spotless:apply` was not run first, `verify` will fail because `spotless:check` runs during the `verify` phase. This is expected behavior.
  - If YAML formatting changed the file content, the `spotless:check` will pass because the files are already formatted (assuming Sub-Task 2 was run).
- **Testing Suggestions:**
  - `./mvnw verify 2>&1 | tail -50`
- **Done When:** `./mvnw verify` completes successfully.

---

## Final Integration & Verification

- **System-Wide Test:** `./mvnw verify` from `app/` — compiles modules, runs Spotless check (all 4 formats), and runs tests.
- **Completion Checklist:**
  - [ ] YAML `<yaml>` block added to `app/pom.xml` (Sub-Task 1).
  - [ ] `<lineEndings>LF</lineEndings>` set at configuration level (if not already present).
  - [ ] `./mvnw spotless:apply` formats YAML files without errors (Sub-Task 2).
  - [ ] YAML files retain valid, semantically correct content (Sub-Task 2).
  - [ ] `./mvnw verify` passes with all checks and tests (Sub-Task 3).

## Open Questions

- *(None — requirements are clear and self-contained.)*
