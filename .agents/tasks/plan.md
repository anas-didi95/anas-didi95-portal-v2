# Plan: Fix CI/CD pipeline issues from code review

## Objective

Fix 3 blocking (P0) issues and clean up dead commented-out code in the new `.github/` CI/CD files so the pipeline is safe and functional.

## Requirements Snapshot

- **R1 (Shell injection fix):** Replace shell-interpolated `${{ github.head_ref }}` / `${{ github.base_ref }}` in `check-pull-request.yml` with a safe `if:` condition to prevent attacker branch name injection via `pull_request_target`.
- **R2 (Maven dependency build):** Add `-am` (also make) to the `mvnw` command in `springboot-ci-uam.yml` so the `common` dependency module is built alongside `uam`.
- **R3 (Artifact path fix):** Correct the `cp` source path in `springboot-ci-uam.yml` from `target/uam/` (which does not exist) to `uam/target/*.jar` (the actual Maven module output).
- **R4 (Remove dead code):** Strip the commented-out `build-web` and `docker-publish` jobs from `springboot-ci-uam.yml` to eliminate maintenance traps.

## Scope

- Edit 2 existing YAML workflow files (no new files).
- Do not touch any Java source code, tests, or other config files.
- Do not modify `CODEOWNERS`, `dependabot.yml`, or any other staged file.

## Assumptions and Constraints

- Working directory for Maven commands in CI is `./app`.
- Spring Boot Maven plugin places the fat JAR in `uam/target/` (the module's own target dir).
- The `Dockerfile.publish` and `web/` directory do not yet exist and are not planned for this feature branch.
- Staged files are the only files to edit (already staged via `git add`).

## Risks and Areas Requiring Care

- Editing files that are already staged means they need to be re-staged after modification. Use `git add` to update the staging area.
- The artifact path fix must match the actual Maven output — the Spring Boot repackaged JAR uses the pattern `<module>/target/<artifact>-<version>.jar`. Using `*.jar` is safe.
- The `if:` condition syntax in `check-pull-request.yml` must use single quotes (YAML-safe) and reference `github.head_ref` / `github.base_ref` without `${{ }}`.

## Sub-Tasks

### Sub-Task 1: Fix shell injection in check-pull-request.yml

- **Status:** Pending
- **Objective:** Replace the unsafe shell `if` block with a workflow-level `if:` condition that cannot be injected.
- **Related Requirements:** R1
- **Dependencies and Preconditions:** None
- **In Scope for This Sub-Task:**
  - Replace the `run:` block (lines 16-19) with an `if:` condition on the step.
  - Keep the `name` and step structure identical.
- **Out of Scope for This Sub-Task:**
  - Any changes to `springboot-ci-uam.yml` or other files.
- **Instructions:**
  1. Remove lines 16-19 (the `run:` block containing shell code).
  2. Add `if: github.head_ref != 'develop' && github.base_ref == 'main'` as a step-level property.
  3. Replace the `run:` block with a simple `run: exit 1` and the existing echo message.
- **Acceptance Criteria:**
  - The workflow step uses `if:` at the step level, never interpolating context variables into a shell string.
  - A PR from any non-develop branch targeting `main` triggers the check and fails.
  - A PR from `develop` to `main` passes.
  - Any PR targeting `develop` passes regardless of source.
- **Cautionary Points (Risks & Edge Cases):**
  - The `if:` condition must use the unadorned `github.head_ref` / `github.base_ref` (no `${{ }}` wrap).
  - Single quotes around `'develop'` and `'main'` are required for YAML string safety.
- **Implementation Suggestions:**
  ```yaml
        - name: Check branches
          if: github.head_ref != 'develop' && github.base_ref == 'main'
          run: |
            echo "Merge requests to main branch are only allowed from develop branch."
            exit 1
  ```
- **Testing Suggestions:** No automated test; verify by inspecting the YAML for absence of `${{ }}` inside `run:` and correct `if:` syntax.
- **Done When:** The file is edited, saved, `git add`-ed, and the YAML is syntactically valid.

---

### Sub-Task 2: Fix Maven build command (add -am flag)

- **Status:** Pending
- **Objective:** Add `-am` to the Maven command so `common` is built first in CI.
- **Related Requirements:** R2
- **Dependencies and Preconditions:** None
- **In Scope for This Sub-Task:**
  - Edit line 42 in `springboot-ci-uam.yml`: change `./mvnw clean package -pl uam` to `./mvnw clean package -pl uam -am`.
- **Out of Scope for This Sub-Task:**
  - Any other command or file edits.
- **Instructions:**
  1. Open `springboot-ci-uam.yml`.
  2. On line 42, change `run: ./mvnw clean package -pl uam` to `run: ./mvnw clean package -pl uam -am`.
- **Acceptance Criteria:**
  - The command reads `./mvnw clean package -pl uam -am`.
  - Matches the canonical form from `AGENTS.md` (`./mvnw compile -pl uam -am`).
- **Cautionary Points (Risks & Edge Cases):**
  - Do not add extra flags or reorder arguments.
- **Testing Suggestions:** Run `./mvnw clean verify -pl uam -am` from `app/` to verify it passes.
- **Done When:** Line is edited, saved, and `git add`-ed.

---

### Sub-Task 3: Fix artifact path in prepare step

- **Status:** Pending
- **Objective:** Correct the `cp` source path to point to the actual Maven build output.
- **Related Requirements:** R3
- **Dependencies and Preconditions:** Sub-Task 2 (artifact appears only after successful Maven build).
- **In Scope for This Sub-Task:**
  - Edit lines 44-46 in `springboot-ci-uam.yml`: replace `cp -Rv target/uam/ artifact/` with `cp uam/target/*.jar artifact/`.
- **Out of Scope for This Sub-Task:**
  - Changing the `mkdir` line or the upload step.
- **Instructions:**
  1. Open `springboot-ci-uam.yml`.
  2. Replace `cp -Rv target/uam/ artifact/` with `cp uam/target/*.jar artifact/`.
  3. Optional: remove the `-v` flag from `mkdir` line — not required but harmless.
- **Acceptance Criteria:**
  - The command copies all JAR files from `uam/target/` into `artifact/`.
  - The upload step's `path: app/artifact/` remains correct.
- **Cautionary Points (Risks & Edge Cases):**
  - If the Spring Boot plugin produces both a plain JAR and an executable JAR, `*.jar` captures both. That is fine.
  - The `working-directory: ./app` makes `uam/target/` resolve correctly to `app/uam/target/`.
- **Testing Suggestions:** After Maven build, verify `ls app/uam/target/*.jar` exists.
- **Done When:** Lines are edited, saved, and `git add`-ed.

---

### Sub-Task 4: Remove commented-out dead code

- **Status:** Pending
- **Objective:** Delete the `build-web` and `docker-publish` commented-out job blocks.
- **Related Requirements:** R4
- **Dependencies and Preconditions:** None
- **In Scope for This Sub-Task:**
  - Remove lines 54-79 (commented `build-web` job).
  - Remove lines 81-133 (commented `docker-publish` job).
- **Out of Scope for This Sub-Task:**
  - Any active code or other files.
- **Instructions:**
  1. Open `springboot-ci-uam.yml`.
  2. Delete lines 54-79 inclusive (the `#build-web:` block).
  3. Delete lines 81-133 inclusive (the `#docker-publish:` block).
  4. The file should end cleanly after line 52 (`retention-days: 1`).
- **Acceptance Criteria:**
  - The file contains only the active `build-app` job.
  - No commented-out job blocks remain.
  - YAML is valid.
- **Cautionary Points (Risks & Edge Cases):**
  - Ensure blank lines between the remaining `build-app` job and end-of-file are clean.
- **Testing Suggestions:** Validate YAML syntax: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/springboot-ci-uam.yml'))"`.
- **Done When:** Lines deleted, saved, `git add`-ed, and YAML syntax check passes.

## Final Integration & Verification

- **Verification Steps:**
  1. Confirm all 4 files are properly staged: `git diff --staged --stat` shows the updated `.github/workflows/check-pull-request.yml` and `springboot-ci-uam.yml`.
  2. Validate YAML syntax on both workflow files.
  3. Run `./mvnw clean verify -pl uam -am` from `app/` to confirm the Maven build works end-to-end.
  4. Review final `git diff --staged` to confirm no unintended changes.
- **Completion Checklist:**
  - [ ] `check-pull-request.yml` uses `if:` condition, no shell injection vector.
  - [ ] `springboot-ci-uam.yml` Maven command includes `-am`.
  - [ ] `springboot-ci-uam.yml` artifact path uses `uam/target/*.jar`.
  - [ ] `springboot-ci-uam.yml` has no commented-out jobs.
  - [ ] All changes staged and ready for commit.
