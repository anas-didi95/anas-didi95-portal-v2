# Plan: Implement CI workflow + Dockerfile fixes from code review

## Objective

Fix all actionable findings from the staged-file code review so the `springboot-ci-uam.yml` CI pipeline and `Dockerfile.publish` work correctly end-to-end. The primary goal is unblocking the `docker-publish` job (two P1 bugs), with secondary hardening for build context hygiene and maintainability (P2/P3 items).

## Requirements Snapshot

- **R1 (Dockerfile path):** Workflow `file:` input must resolve to the actual `Dockerfile.publish` location (`app/uam/Dockerfile.publish`), not a non-existent root-level path.
- **R2 (JAR staging):** The Docker build context must contain `app/uam/target/*.jar` so the Dockerfile's `COPY target/*.jar application.jar` resolves during `docker build`.
- **R3 (`.dockerignore`):** Add a `.dockerignore` at the repo root to prevent sending unnecessary files to the Docker daemon.
- **R4 (Sparse checkout):** The sparse-checkout strategy in the `docker-publish` job should be simplified for clarity and robustness.
- **R5 (Cosign polishing):** Add explicit OIDC identity token retrieval to the cosign signing step for reliable keyless signing.
- **R6 (Conditional logic):** Replace the `IS_PUSH_IMAGE` env var with inline `github.event_name` checks to improve readability.
- **R7 (Use env var for module name):** All workflow file paths referencing the app module must use `${{ env.APP_NAME }}` instead of the hardcoded string `uam`, so the workflow stays generic. Exception: `.dockerignore` (interpreted by Docker, not GitHub Actions) must use the literal path.

## Scope

- Edit `.github/workflows/springboot-ci-uam.yml` to fix all findings.
- Create `.dockerignore` at repo root.
- No changes to `app/uam/Dockerfile.publish` (the Dockerfile itself is correct — only the CI workflow feeding it is wrong).

## Assumptions and Constraints

- The repo is small; a full checkout in the `docker-publish` job is negligible performance-wise and simpler than sparse checkout.
- The Dockerfile's `ARG JAR_FILE=target/*.jar` will remain unchanged — staging is handled in the workflow.
- All changes are on the current branch (`feature/user-service`), target for merge into `develop`.
- The workflow is new (no existing CI files), so no backward-compatibility concerns.
- **GitHub expressions do not work in `.dockerignore`** — it is parsed by Docker, not GitHub Actions. Any path in `.dockerignore` must be a literal file-system path (e.g. `app/uam/`). Hardcoding `uam` there is unavoidable.
- **All other file paths in `.github/workflows/`** must use `${{ env.APP_NAME }}` instead of the literal `uam`. This keeps the workflow reusable if the module is ever renamed.

## Risks and Areas Requiring Care

- **Order of steps in `docker-publish`:** The JAR staging step must run *after* `actions/download-artifact@v4` but *before* `docker/build-push-action`.
- **Path correctness:** The `cp` glob in the staging step must match whatever filename the Maven build produces. `*.jar` covers the standard `uam-<version>.jar` or `uam.jar`.
- **`.dockerignore` pattern order:** Deny-first (`*`) with selective allows is correct, but the allowed paths must match the Docker build context structure.
- **`${{ env.APP_NAME }}` everywhere:** Any remaining hardcoded `uam` in the workflow YAML that should reference `${{ env.APP_NAME }}` will silently work for the current module but break reusability. Audit all path references.

## Sub-Tasks

### Sub-Task 1: Fix Dockerfile path and add JAR staging step

- **Status:** Completed
- **Objective:** Resolve both P1 issues so the `docker-publish` job can build the Docker image.
- **Related Requirements:** R1, R2
- **Dependencies and Preconditions:** None (straightforward edits).
- **In Scope for This Sub-Task:**
  - Change `file: Dockerfile.publish` → `file: app/${{ env.APP_NAME }}/Dockerfile.publish` on line 94.
  - Add a new step named `Stage artifact for Docker build` between the `actions/download-artifact@v4` step and the `Display structure of downloaded files` step (or between the display step and `Install cosign`).
  - The staging step must create `app/${{ env.APP_NAME }}/target/` and copy the JAR from the downloaded artifact into it.
- **Out of Scope for This Sub-Task:**
  - `.dockerignore`, sparse checkout, cosign, or `IS_PUSH_IMAGE` changes.
- **Instructions:**
  1. Open `.github/workflows/springboot-ci-uam.yml`.
  2. On line 94, change `file: Dockerfile.publish` to `file: app/${{ env.APP_NAME }}/Dockerfile.publish`.
  3. After the `Display structure of downloaded files` step (line 69-70), add:
     ```yaml
     - name: Stage artifact for Docker build
       run: |
         mkdir -p app/${{ env.APP_NAME }}/target
         cp app-${{ env.APP_NAME }}/*.jar app/${{ env.APP_NAME }}/target/
     ```
  4. Ensure the indentation is consistent (2-space, same as other steps in the job).
- **Acceptance Criteria:**
  - Line 94 references `app/${{ env.APP_NAME }}/Dockerfile.publish` (not hardcoded `uam`).
  - A staging step exists that copies the JAR into `app/${{ env.APP_NAME }}/target/`.
  - The staging step runs before `docker/build-push-action`.
- **Cautionary Points:**
  - The staging step's `working-directory` defaults to repo root (no `defaults.run.working-directory` set in the `docker-publish` job). This is correct since both the checkout and downloaded artifact are at the repo root.
- **Testing Suggestions:** After applying the edit, inspect the file to confirm the changes. No runtime test possible without pushing to a branch and triggering the workflow.
- **Done When:** Both edits are applied and verified by reading the file.

### Sub-Task 2: Create `.dockerignore` at repo root

- **Status:** Completed
- **Objective:** Prevent unnecessary files from being sent to the Docker daemon during `docker build`.
- **Related Requirements:** R3
- **Dependencies and Preconditions:** Sub-Task 1 (the `.dockerignore` paths must align with the chosen artifact staging approach).
- **In Scope for This Sub-Task:**
  - Create a `.dockerignore` file at `/home/vscode/workspace/.dockerignore`.
  - The `.dockerignore` should deny all (`*`) then selectively allow only what the Docker build needs:
    - `app/uam/Dockerfile.publish` (must be hardcoded — `.dockerignore` is parsed by Docker, not GitHub Actions)
    - `app/uam/target/*.jar` (same reason)
- **Out of Scope for This Sub-Task:** Adding `.dockerignore` to `.gitignore` (it should be tracked).
- **Instructions:**
  ⚠️ **Important:** The `.dockerignore` is processed by the Docker CLI, *not* by GitHub Actions. Therefore `${{ env.APP_NAME }}` does **not** work here. Use the literal path `app/uam/`.
  
  Create `.dockerignore` with:
  ```
  *
  !app/uam/Dockerfile.publish
  !app/uam/target/
  !app/uam/target/*.jar
  ```
- **Acceptance Criteria:**
  - `.dockerignore` exists at the repo root.
  - The file contains the deny-first pattern with allows for the Dockerfile and the staged JAR directory.
- **Cautionary Points:**
  - Trailing `/` on `!app/uam/target/` allows the directory itself (needed for the glob to work).
  - The `!app/uam/target/*.jar` line ensures `.jar` files inside are not denied.
- **Testing Suggestions:** Confirm file exists with `ls -la .dockerignore`.
- **Done When:** The `.dockerignore` file is created with the correct content.

### Sub-Task 3: Simplify sparse checkout strategy

- **Status:** Completed
- **Objective:** Replace the non-cone-mode sparse checkout with a full checkout for reliability and simplicity.
- **Related Requirements:** R4
- **Dependencies and Preconditions:** Sub-Task 1 (since the Dockerfile path in the workflow will reference `app/${{ env.APP_NAME }}/Dockerfile.publish`, a full checkout is harmless).
- **In Scope for This Sub-Task:**
  - Remove the `sparse-checkout` and `sparse-checkout-cone-mode` parameters from the checkout step (lines 64-66).
  - The step should be a plain `uses: actions/checkout@v4` with no `with:` block (or an empty `with:` block).
- **Out of Scope for This Sub-Task:** Any other checkout optimizations.
- **Instructions:**
  1. Open `.github/workflows/springboot-ci-uam.yml`.
  2. Replace lines 63-66:
     ```yaml
         - name: Checkout Dockerfile
           uses: actions/checkout@v4
           with:
             sparse-checkout: app/${{ env.APP_NAME }}/Dockerfile.publish
             sparse-checkout-cone-mode: false
     ```
     with:
     ```yaml
         - name: Checkout repository
           uses: actions/checkout@v4
     ```
- **Acceptance Criteria:**
  - The checkout step no longer has `sparse-checkout` or `sparse-checkout-cone-mode` keys.
  - The step name reflects that it checks out the full repository.
- **Cautionary Points:** A full checkout is fine for this repo's size.
- **Testing Suggestions:** Visual inspection of the edited file.
- **Done When:** The edit is applied and verified.

### Sub-Task 4: Harden cosign signing with explicit OIDC identity token

- **Status:** Completed
- **Objective:** Make keyless signing reliable by explicitly retrieving and passing the OIDC identity token.
- **Related Requirements:** R5
- **Dependencies and Preconditions:** Sub-Task 1 (base workflow structure in place).
- **In Scope for This Sub-Task:**
  - Add a step to request the OIDC JWT token before the signing step.
  - Modify the `cosign sign` command to pass `--identity-token`.
- **Out of Scope for This Sub-Task:** Switching to the `cosign-action` GitHub Action.
- **Instructions:**
  1. Open `.github/workflows/springboot-ci-uam.yml`.
  2. Before the `Sign the published Docker image` step (before line 100), add:
     ```yaml
         - name: Retrieve OIDC token
           id: auth
           uses: actions/github-script@v7
           with:
             retries: 2
             script: |
               const token = process.env.ACTIONS_ID_TOKEN_REQUEST_TOKEN;
               const repoPath = process.env.GITHUB_REPOSITORY_OWNER + '/' + process.env.GITHUB_REPOSITORY;
               const res = await fetch(
                 `${process.env.ACTIONS_ID_TOKEN_REQUEST_URL}&audience=sigstore`
               );
               const data = await res.json();
               core.setOutput('oidc', data.value);
     ```
  3. On line 105, change the `cosign sign` command to:
     ```yaml
         run: echo "${TAGS}" | xargs -I {} cosign sign --yes --identity-token ${{ steps.auth.outputs.oidc }} {}@${DIGEST}
     ```
- **Acceptance Criteria:**
  - A `Retrieve OIDC token` step exists before the signing step.
  - The `cosign sign` command includes `--identity-token ${{ steps.auth.outputs.oidc }}`.
- **Cautionary Points:**
  - The `id-token: write` permission is already declared (line 60), so OIDC token retrieval will work.
  - The `actions/github-script@v7` uses Node's native `fetch` (available in Node 20+ which is the default runner).
- **Testing Suggestions:** Visual inspection of the edited file.
- **Done When:** Both edits are applied and verified.

### Sub-Task 5: Simplify `IS_PUSH_IMAGE` logic by using inline conditions

- **Status:** Completed
- **Objective:** Remove the `IS_PUSH_IMAGE` env var and use `github.event_name` directly for clarity.
- **Related Requirements:** R6
- **Dependencies and Preconditions:** Sub-Task 1 (base workflow structure in place).
- **In Scope for This Sub-Task:**
  - Remove the `IS_PUSH_IMAGE` line from the `env:` block (line 23).
  - Replace `if: env.IS_PUSH_IMAGE == 'true'` → `if: github.event_name != 'pull_request'` on lines 78 and 101.
  - Replace `push: ${{ env.IS_PUSH_IMAGE == 'true' }}` → `push: ${{ github.event_name != 'pull_request' }}` on line 95.
- **Out of Scope for This Sub-Task:** Any other env var changes.
- **Instructions:**
  1. Delete line 23: `IS_PUSH_IMAGE: ${{ github.event_name != 'pull_request' }}`.
  2. On line 78, change: `if: env.IS_PUSH_IMAGE == 'true'` → `if: github.event_name != 'pull_request'`.
  3. On line 95, change: `push: ${{ env.IS_PUSH_IMAGE == 'true' }}` → `push: ${{ github.event_name != 'pull_request' }}`.
  4. On line 101, change: `if: env.IS_PUSH_IMAGE == 'true'` → `if: github.event_name != 'pull_request'`.
- **Acceptance Criteria:**
  - The `IS_PUSH_IMAGE` env var is removed from the `env:` block.
  - All three usages of `env.IS_PUSH_IMAGE` are replaced with `github.event_name != 'pull_request'`.
  - The logic is identical: push and dispatch runs push images; PR runs do not.
- **Cautionary Points:**
  - Line 95 is inside a `with:` block, so the expression uses `${{ }}` syntax (it already did). The replacement is straightforward.
  - Lines 78 and 101 are `if:` conditions at the step level, not inside `${{ }}`.
- **Testing Suggestions:** Visual inspection of the edited file.
- **Done When:** All four edits are applied and verified.

## Final Integration & Verification

- **File Integrity Check:** Read the final `.github/workflows/springboot-ci-uam.yml` and confirm:
  - The env block no longer contains `IS_PUSH_IMAGE`.
  - The checkout step is a simple `actions/checkout@v4`.
  - A staging step copies the JAR into `app/${{ env.APP_NAME }}/target/` (uses env var, not hardcoded `uam`).
  - The Docker build `file:` points to `app/${{ env.APP_NAME }}/Dockerfile.publish` (uses env var).
  - The cosign step has OIDC token retrieval before it.
- **Workflow Path Audit:** Search the entire workflow file for any remaining instance of the literal `uam` in a path. If one is found that should reference `${{ env.APP_NAME }}`, fix it. The only acceptable literal `uam` in the workflow is in the workflow name (`Spring Boot CI with Maven - UAM`) and the `APP_NAME: uam` env var definition itself.
- **New File Check:** Confirm `.dockerignore` exists at the repo root.
- **Format Check:** Run `./mvnw spotless:apply` from `app/` (unlikely to affect YAML, but good practice).
- **Git Status:** Verify only the intended files are staged:
  - `.github/workflows/springboot-ci-uam.yml` (modified)
  - `.dockerignore` (new)
- **Completion Checklist:**
  - [x] Sub-Task 1: Dockerfile path + JAR staging step
  - [x] Sub-Task 2: `.dockerignore` created
  - [x] Sub-Task 3: Sparse checkout simplified
  - [x] Sub-Task 4: Cosign OIDC token hardening
  - [x] Sub-Task 5: Inline event conditions instead of `IS_PUSH_IMAGE`

## Open Questions

- None. All findings from the review have concrete fixes with clear guidance.
