# Code Review Summary

**Scope**: CI/CD infrastructure (4 new files under `.github/`)
**Branch**: `feature/user-service`
**Overall risk**: Medium
**Verdict**: Request changes

---

## Findings

### [P0] Blocking

#### Shell injection vulnerability in PR branch check
- **Location**: `.github/workflows/check-pull-request.yml:48-52`
- **Why it matters**: `pull_request_target` runs with full repo write permissions in the base repo's context. Unsanitized `${{ github.head_ref }}` / `${{ github.base_ref }}` expressions in a shell script allow an attacker to inject arbitrary commands via a crafted branch name (e.g., a branch named `develop" ] && curl ... #`), bypassing the gate or exfiltrating secrets.
- **Evidence**: The `run:` block interpolates `${{ github.head_ref }}` and `${{ github.base_ref }}` directly into a shell `if` statement. GitHub Actions expands these before the shell evaluates them, and `pull_request_target` runs in the context of the target repo, giving write access to `GITHUB_TOKEN`.
- **Fix**: Replace the shell script with a workflow-level `if:` condition that uses the structured `github` context (never interpolated into a shell):
  
  ```yaml
  - name: Check branches
    if: github.head_ref != 'develop' && github.base_ref == 'main'
    run: |
      echo "Merge requests to main branch are only allowed from develop branch."
      exit 1
  ```

#### Maven build will fail in CI — missing dependency module
- **Location**: `.github/workflows/springboot-ci-uam.yml:101`
- **Why it matters**: The `uam` module depends on `common`, which is a sibling Maven module. In a fresh CI checkout, `common` has never been installed into the local Maven repo. Building `-pl uam` without `-am` (also make) will fail with a dependency resolution error.
- **Evidence**: `AGENTS.md` shows the canonical compile command is `./mvnw compile -pl uam -am` and the test command is `./mvnw test -pl uam -am` — both use `-am`. The CI command `./mvnw clean package -pl uam` omits `-am`.
- **Fix**: Change line 101 to:
  
  ```yaml
  run: ./mvnw clean package -pl uam -am
  ```

#### Artifact path does not exist — `target/uam/` is not a Maven build output
- **Location**: `.github/workflows/springboot-ci-uam.yml:103-105`
- **Why it matters**: After building the `uam` module, the build output goes to `app/uam/target/` (the module's target directory), not `app/target/uam/`. The `cp -Rv target/uam/ artifact/` command will fail because the source directory does not exist, breaking the entire pipeline.
- **Evidence**: Maven multi-module builds place each module's output in `<module-dir>/target/`. Spring Boot fat JARs land at `uam/target/uam-<version>.jar` or `uam/target/*.jar`. The directory `target/uam/` is never created by a standard Maven build.
- **Fix**: Either:
  - Copy the JAR directly: `cp uam/target/*.jar artifact/`, or
  - Adjust the path to match the actual module output structure, e.g.:
    
    ```yaml
    - name: Prepare artifact
      run: |
        mkdir artifact
        cp uam/target/*.jar artifact/
    ```

### [P2] Medium

#### Commented-out build-web job references non-existent `web/` directory
- **Location**: `.github/workflows/springboot-ci-uam.yml:113-138`
- **Why it matters**: If uncommented without updating the path, this job would fail on `working-directory: ./web` since `web/` does not exist in the repository. Not blocking (fully commented out), but creates a maintenance trap.
- **Evidence**: No `web/` directory exists at the repo root.
- **Fix**: Remove the commented-out job entirely, or update it to reflect future intent (e.g., adjust paths or add a TODO with more context).

#### Commented-out Docker publish references missing `Dockerfile.publish`
- **Location**: `.github/workflows/springboot-ci-uam.yml:140-192`
- **Why it matters**: Same pattern as above — `Dockerfile.publish` and sparse checkout will fail when uncommented.
- **Evidence**: No `Dockerfile.publish` exists at the repo root.
- **Fix**: Either create the Dockerfile before uncommenting, or remove the commented block.

---

## Suggested Next Steps

- [ ] **Fix P0 findings** before merging:
  - Protect the branch check shell script from injection (use `if:` condition).
  - Add `-am` flag to the Maven build command.
  - Fix the artifact path to point to the actual build output.
- [ ] Remove commented-out job blocks that reference non-existent resources, or create the required files first.
- [ ] Re-run the full pipeline locally (`./mvnw clean verify -pl uam -am`) before merge to confirm the Maven fix works.
