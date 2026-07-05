# Plan: Change Docker build context from repo root to `app/uam`

## Objective

Change the Docker build context from repo root (`.`) to `app/uam` so the Dockerfile's `COPY target/*.jar` resolves correctly without modifying the Dockerfile. The JAR is staged at `app/uam/target/` by the CI workflow, so setting context to `app/uam` makes `target/*.jar` the correct relative path.

## Requirements Snapshot

- **R1 (Change context in workflow):** Update `docker/build-push-action` input `context: .` → `context: app/${{ env.APP_NAME }}`.
- **R2 (No change to Dockerfile):** The Dockerfile must not be modified. It already uses `ARG JAR_FILE=target/*.jar` — this must remain as-is.
- **R3 (New `.dockerignore`):** Since Docker reads `.dockerignore` from the **context root**, create `app/uam/.dockerignore` to control what gets sent to the Docker daemon.
- **R4 (Revert prior edit):** The Dockerfile was previously edited to `ARG JAR_FILE=app/uam/target/*.jar`. Revert to the original `target/*.jar`.
- **R5 (Remove orphaned root `.dockerignore`):** The root `.dockerignore` was created for the old `context: .` approach. After the context change, it is unused. Remove it.
- **R6 (Use `${{ env.APP_NAME }}` everywhere):** The workflow must use `${{ env.APP_NAME }}` instead of hardcoded `uam` in all path references.

## Scope

- Edit `.github/workflows/springboot-ci-uam.yml` — change `context: .` to `context: app/${{ env.APP_NAME }}`.
- Create `app/uam/.dockerignore` — deny-first with allows for `Dockerfile.publish`, `target/`, and `target/*.jar`.
- Revert `app/uam/Dockerfile.publish` — change `ARG JAR_FILE=app/uam/target/*.jar` back to `ARG JAR_FILE=target/*.jar`.
- **Remove** root `.dockerignore` — no longer needed.

## Assumptions and Constraints

- The CI workflow stages the JAR at `app/uam/target/*.jar` relative to repo root (line 71 of workflow).
- With context `app/uam`, this path maps to `target/*.jar` inside the context — exactly what the Dockerfile expects.
- The `file:` parameter of `docker/build-push-action` is relative to the repo root, not the context — so `file: app/${{ env.APP_NAME }}/Dockerfile.publish` stays correct.
- **GitHub Actions expressions like `${{ env.APP_NAME }}` do NOT work in `.dockerignore`** — it is parsed by Docker CLI, not GitHub Actions. The new `app/uam/.dockerignore` does not need them anyway: all its paths are relative to the context root (`!target/`, `!Dockerfile.publish`) and contain no module-name prefix.
- The only Docker build in the project uses this workflow, so the root `.dockerignore` has no other consumers after the context change.

## Risks and Areas Requiring Care

- **`docker/build-push-action` behavior:** When `context: app/uam` is set, the action tars up the `app/uam/` directory as the build context. Docker reads `app/uam/.dockerignore` (not the root `.dockerignore`) from that context. Both the `.dockerignore` and the context must be consistent.
- **The `file:` parameter is relative to repo root** per `docker/build-push-action` docs — it does NOT change when context changes. Keep `file: app/${{ env.APP_NAME }}/Dockerfile.publish`.
- **Cosign OIDC step** (line 102-113) is unrelated and must not be touched.
- **Removing root `.dockerignore`** is safe only because no other Dockerfiles or workflows reference it. The glob search confirms `app/uam/Dockerfile.publish` is the only Dockerfile in the repo.

## Sub-Tasks

### Sub-Task 1: Revert Dockerfile `JAR_FILE` to original

- **Status:** Pending
- **Objective:** Undo the previous edit that changed `ARG JAR_FILE=target/*.jar` to `ARG JAR_FILE=app/uam/target/*.jar`.
- **Related Requirements:** R2, R4
- **Dependencies and Preconditions:** None.
- **In Scope for This Sub-Task:**
  - Revert line 7 of `app/uam/Dockerfile.publish` from `ARG JAR_FILE=app/uam/target/*.jar` back to `ARG JAR_FILE=target/*.jar`.
- **Out of Scope for This Sub-Task:**
  - Any other changes to the Dockerfile (lines 1-6, 8-34 must remain untouched).
- **Instructions:**
  1. Open `app/uam/Dockerfile.publish`.
  2. Change line 7 from `ARG JAR_FILE=app/uam/target/*.jar` to `ARG JAR_FILE=target/*.jar`.
- **Acceptance Criteria:**
  - Line 7 reads `ARG JAR_FILE=target/*.jar`.
  - No other lines changed.
- **Cautionary Points:**
  - The glob `target/*.jar` is a Dockerfile glob, resolved relative to the build context. After the context is changed to `app/uam`, this correctly resolves to `app/uam/target/*.jar` (repo-relative).
- **Testing Suggestions:** Read the file to confirm the revert.
- **Done When:** Line 7 shows `ARG JAR_FILE=target/*.jar`.

### Sub-Task 2: Create `app/uam/.dockerignore`

- **Status:** Pending
- **Objective:** Create a `.dockerignore` inside the new context directory so Docker only receives the necessary files.
- **Related Requirements:** R3
- **Dependencies and Preconditions:** None (independent of other changes).
- **In Scope for This Sub-Task:**
  - Create `/home/vscode/workspace/app/uam/.dockerignore`.
  - Use deny-first pattern: deny all (`*`), then selectively allow only what the Docker build needs.
- **Out of Scope for This Sub-Task:**
  - Editing the root `.dockerignore` (handled in Sub-Task 4).
  - Adding `.dockerignore` to `.gitignore`.
- **Instructions:**
  ⚠️ **Important:** This `.dockerignore` is read by Docker CLI relative to the build context root (`app/uam/`). Paths are relative to the context, not the repo root. Do NOT use `app/uam/` prefixes here. No `${{ env.APP_NAME }}` needed since paths are relative (no module name in them).
  
  Create `app/uam/.dockerignore` with:
  ```
  *
  !Dockerfile.publish
  !target/
  !target/*.jar
  ```

  Explanation of each line:
  - `*` — deny everything by default
  - `!Dockerfile.publish` — allow the Dockerfile reference
  - `!target/` — allow the target directory (needed so the glob can descend into it)
  - `!target/*.jar` — allow the JAR files inside target
- **Acceptance Criteria:**
  - File exists at `app/uam/.dockerignore`.
  - Four lines with the deny-first pattern.
  - Paths are relative to `app/uam/` (no `app/uam/` prefix, no `${{ }}` expressions).
- **Cautionary Points:**
  - Docker ignores `.dockerignore` files in parent directories — it only reads the one in the context root. So the root `.dockerignore` is irrelevant for this build.
  - This file should be tracked in git.
- **Testing Suggestions:** Confirm file exists with `ls -la app/uam/.dockerignore`.
- **Done When:** The `.dockerignore` file is created with correct content.

### Sub-Task 3: Update workflow context from `.` to `app/uam`

- **Status:** Pending
- **Objective:** Change the Docker build context in the CI workflow so the Dockerfile's COPY resolves correctly.
- **Related Requirements:** R1, R6
- **Dependencies and Preconditions:** Sub-Task 1 (Dockerfile reverted) and Sub-Task 2 (`.dockerignore` created at context root) should be done first so the workflow points to a consistent context.
- **In Scope for This Sub-Task:**
  - Edit line 94 of `.github/workflows/springboot-ci-uam.yml`: change `context: .` → `context: app/${{ env.APP_NAME }}`.
- **Out of Scope for This Sub-Task:**
  - Changing the `file:` parameter (line 95 stays `file: app/${{ env.APP_NAME }}/Dockerfile.publish` — it's relative to repo root and already uses `${{ env.APP_NAME }}`).
  - Any other workflow edits.
- **Instructions:**
  1. Open `.github/workflows/springboot-ci-uam.yml`.
  2. Find the `Build and push Docker image` step (line 91-100).
  3. On line 94, change `context: .` to `context: app/${{ env.APP_NAME }}`.
  4. Leave all other lines in the step unchanged.
- **Acceptance Criteria:**
  - Line 94 reads `context: app/${{ env.APP_NAME }}` (uses env var, not hardcoded `uam`).
  - Line 95 still reads `file: app/${{ env.APP_NAME }}/Dockerfile.publish` (already uses env var).
  - No other changes in the file.
- **Cautionary Points:**
  - The `file:` parameter is documented as relative to the **repository root** in `docker/build-push-action`, so it does not change when context changes. It must stay as `app/${{ env.APP_NAME }}/Dockerfile.publish`.
  - The staging step (line 67-71) places the JAR at `app/${{ env.APP_NAME }}/target/` relative to repo root — already uses `${{ env.APP_NAME }}`. With context `app/uam`, this maps to `target/` inside the context — correct.
- **Testing Suggestions:** Read the edited file to confirm both lines 94 and 95 use `${{ env.APP_NAME }}` and no hardcoded `uam`.
- **Done When:** Line 94 shows `context: app/${{ env.APP_NAME }}`.

### Sub-Task 4: Remove orphaned root `.dockerignore`

- **Status:** Pending
- **Objective:** Remove the root `.dockerignore` file that was created for the old `context: .` approach. It is no longer used after the context change.
- **Related Requirements:** R5
- **Dependencies and Preconditions:** Sub-Task 2 (new `.dockerignore` at `app/uam/` is created and working) and Sub-Task 3 (context is changed so root `.dockerignore` is no longer consumed).
- **In Scope for This Sub-Task:**
  - Delete `/home/vscode/workspace/.dockerignore`.
- **Out of Scope for This Sub-Task:**
  - Any other file cleanup.
- **Instructions:**
  1. Remove the file at `/home/vscode/workspace/.dockerignore`.
- **Acceptance Criteria:**
  - Root `.dockerignore` does not exist.
  - No other files changed.
- **Cautionary Points:**
  - Confirm no other Dockerfiles or workflows reference the root `.dockerignore`. The glob search shows only `app/uam/Dockerfile.publish` exists, and this workflow is the only CI build. Removal is safe.
- **Testing Suggestions:** Run `ls -la /home/vscode/workspace/.dockerignore` and confirm "No such file or directory".
- **Done When:** The root `.dockerignore` is deleted.

## Final Integration & Verification

- **Path Trace Verification:** Verify the full end-to-end path resolution:
  1. Staging step (line 71): `cp app-${{ env.APP_NAME }}/*.jar app/${{ env.APP_NAME }}/target/` → JAR at `app/uam/target/` (repo-relative)
  2. Docker context: `app/${{ env.APP_NAME }}` → context root is `app/uam/`
  3. Dockerfile reads `.dockerignore` at `app/uam/.dockerignore` → allows `target/*.jar`
  4. Dockerfile COPY: `COPY target/*.jar application.jar` → resolves to `app/uam/target/*.jar` (repo-relative) ✓
- **Env Var Audit:** Scan the workflow for hardcoded `uam` in path references:
  - `APP_NAME: uam` in `env:` — this is the definition, acceptable (required to set the variable)
  - All other path references (context, file, artifact name, staging paths) must use `${{ env.APP_NAME }}`
  - The only acceptable literal `uam` in the workflow is the `APP_NAME: uam` definition and the workflow name string
- **File Audit:**
  - `app/uam/Dockerfile.publish`: line 7 must be `ARG JAR_FILE=target/*.jar` (not `app/uam/target/...`)
  - `.github/workflows/springboot-ci-uam.yml`: line 94 must be `context: app/${{ env.APP_NAME }}`
  - `app/uam/.dockerignore`: must exist with correct paths
  - Root `.dockerignore`: must NOT exist (removed)
- **Git Status:** Only intended files changed:
  - `.github/workflows/springboot-ci-uam.yml` (modified — context change)
  - `app/uam/Dockerfile.publish` (modified — revert JAR_FILE)
  - `app/uam/.dockerignore` (new)
  - `.dockerignore` (deleted — root `.dockerignore` removed)
- **Completion Checklist:**
  - [ ] Sub-Task 1: Dockerfile JAR_FILE reverted
  - [ ] Sub-Task 2: `app/uam/.dockerignore` created
  - [ ] Sub-Task 3: Workflow context changed (uses `${{ env.APP_NAME }}`)
  - [ ] Sub-Task 4: Root `.dockerignore` removed

## Open Questions

- None. All four changes are self-contained with clear verification steps.
