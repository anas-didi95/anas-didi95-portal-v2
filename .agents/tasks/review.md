# Code Review Summary

**Scope**: CI workflow (`.github/workflows/springboot-ci-uam.yml`) + Dockerfile (`app/uam/Dockerfile.publish`)
**Overall risk**: High
**Verdict**: Request changes

## Findings

### [P1] High

- **Dockerfile path mismatch in workflow**
  - **Location**: `.github/workflows/springboot-ci-uam.yml:94`
  - **Why it matters**: The `docker-publish` job will fail with a `COPY failed` / file-not-found error every time it runs.
  - **Evidence**: Line 94 specifies `file: Dockerfile.publish`, but the sparse checkout (line 65) checks the file into `app/uam/Dockerfile.publish`. With `context: .` (repo root), Docker looks for `./Dockerfile.publish` — which does not exist. The correct relative path from context root is `app/uam/Dockerfile.publish`.
  - **Fix**: Change `file: Dockerfile.publish` to `file: app/uam/Dockerfile.publish` on line 94.

- **Docker build cannot find the JAR artifact**
  - **Location**: `app/uam/Dockerfile.publish:7,10` + workflow `docker-publish` job (lines 62–99)
  - **Why it matters**: The Docker build expects the JAR at `target/*.jar` relative to the build context, but the CI workflow never places it there — the artifact is downloaded into an `app-uam/` directory instead.
  - **Evidence**:
    - `build-app` (line 46): `cp uam/target/*.jar artifact/` → JAR is inside `artifact/`, then uploaded with name `app-uam` (line 50).
    - `docker-publish` (line 69): `actions/download-artifact@v4` restores the artifact into a directory `./app-uam/` at the workspace root.
    - Dockerfile line 7: `ARG JAR_FILE=target/*.jar` — no `target/` directory exists in the Docker build context, so `COPY` silently matches nothing (or fails with a glob-no-match error in newer Docker versions).
  - **Fix options** (choose one):
    1. **(Recommended)** In the `docker-publish` job, before the Docker build step, restructure the downloaded artifact so the JAR is at `target/*.jar`:
       ```yaml
       - name: Stage artifact for Docker build
         run: |
           mkdir -p app/${{ env.APP_NAME }}/target
           cp app-${{ env.APP_NAME }}/*.jar app/${{ env.APP_NAME }}/target/
       ```
       Then set `context: .` remains fine, `file: app/uam/Dockerfile.publish`, and the Dockerfile's `target/*.jar` glob resolves.
    2. **Alternatively**, change the Dockerfile to accept the JAR path as a build arg and pass it from the workflow. This adds complexity with less benefit.

### [P2] Medium

- **No `.dockerignore` file**
  - **Location**: repo root (missing file)
  - **Why it matters**: The Docker build context sent to the daemon could be large, slowing builds and increasing layer-cache pressure.
  - **Evidence**: `docker/build-push-action` at line 91 uses `context: .`. Without a `.dockerignore`, the entire checkout (git history, node modules if any, other modules) is sent. Currently mitigated by sparse checkout, but this is a fragile coupling — any change to checkout strategy could silently bloat context.
  - **Fix**: Create a `.dockerignore` at the repo root:
    ```
    *
    !app/uam/Dockerfile.publish
    !app/uam/target/
    ```
    (Adjust paths based on the chosen fix for the artifact staging issue above.)

- **Sparse checkout with cone-mode disabled is brittle**
  - **Location**: `.github/workflows/springboot-ci-uam.yml:65-66`
  - **Why it matters**: `sparse-checkout-cone-mode: false` with a single file path works but is not the standard or well-documented pattern. Future maintainers may misinterpret it.
  - **Evidence**: The standard `sparse-checkout` usage in `actions/checkout@v4` recommends cone mode (default). Disabling it to check out a single file is unusual and fragile across action versions.
  - **Fix**: Either omit sparse checkout entirely (a full checkout is negligible for this size repo) or use a standard cone-mode sparse checkout with a broader pattern.

### [P3] Low

- **Cosign signing step lacks explicit identity flag**
  - **Location**: `.github/workflows/springboot-ci-uam.yml:105`
  - **Why it matters**: Keyless signing relies on ambient OIDC token detection, which can silently fall back to interactive mode or fail in some runner environments.
  - **Evidence**: The `cosign sign` command (line 105) has no `--identity-token` or `--oidc-provider` flag.
  - **Fix**: Add `--identity-token ${{ steps.auth.outputs.oidc }}` after retrieving the token, or use the `cosign-action` in keyless mode which handles this automatically.

- **`IS_PUSH_IMAGE` env var uses GitHub expression in env block**
  - **Location**: `.github/workflows/springboot-ci-uam.yml:23`
  - **Why it matters**: Setting an env var to a boolean expression is fragile — GitHub Actions evaluates it as a string (`'true'` / `'false'`). The downstream `if: env.IS_PUSH_IMAGE == 'true'` works today but is less idiomatic than a direct `if: github.event_name != 'pull_request'`.
  - **Evidence**: Line 23 defines `IS_PUSH_IMAGE: ${{ github.event_name != 'pull_request' }}`. Lines 78 and 95 compare `env.IS_PUSH_IMAGE == 'true'`. This works but is harder to debug and unnecessary.
  - **Fix**: Replace the env var and use `if: github.event_name != 'pull_request'` directly on lines 78 and 95.

## Suggested Next Steps

- [x] Fix P1 findings (Dockerfile path + JAR staging) before merge
- [ ] Add a `.dockerignore` to keep Docker builds lean
- [ ] Consider simplifying sparse checkout strategy
- [ ] Re-test the full pipeline end-to-end after fixes (simulate with `workflow_dispatch` on a branch)
