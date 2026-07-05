# Plan: Let cosign auto-detect OIDC token in GitHub Actions

## Objective

Remove the bespoke "Retrieve OIDC token" script step and let cosign v2 natively auto-discover the GitHub Actions OIDC environment, eliminating the root cause of the `"requires at least 1 arg(s)"` error.

## Requirements Snapshot

- **R1:** The `cosign sign` command must succeed for non-PR pushes (branches `main`/`develop` and tags `v*.*.*`) by signing the published Docker image digest.
- **R2:** The `cosign sign` command must not run on pull_request events (existing `if:` guard preserved).
- **R3:** The signing step must handle multiple tags from `docker/metadata-action` (newline-separated) correctly.

## Scope

- Remove the `Retrieve OIDC token` step (lines 101–113).
- Simplify the `Sign the published Docker image` step by dropping `--identity-token` and the `${{ steps.auth.outputs.oidc }}` reference.
- Keep all other steps, permissions, and guards unchanged.
- Confirm the `id-token: write` permission (line 59) is sufficient for cosign auto-detection.

## Assumptions and Constraints

- **Cosign v2.2.4** (pinned via `cosign-release: "v2.2.4"`) natively reads `ACTIONS_ID_TOKEN_REQUEST_URL` and `ACTIONS_ID_TOKEN_REQUEST_TOKEN` in GitHub Actions environments and auto-requests a token with the `sigstore` audience. No manual token fetch is needed.
- The existing `id-token: write` permission on the `docker-publish` job is already present and correct.
- The `if: github.event_name != 'pull_request'` guard on the sign step is preserved and sufficient.
- The `TAGS` env var from `docker/metadata-action` is newline-separated (one tag per line).
- This is a YAML-only change — no Java, Dockerfile, or configuration changes are needed.

## Risks and Areas Requiring Care

- If cosign auto-detection fails (e.g., environment variables not set), the sign step will error. This is the same failure mode as today, but without the intermediate (broken) script step. The error message from cosign will be more direct.
- Multi-line `TAGS`: `xargs -I {}` correctly invokes cosign once per tag line, each signing `tag@digest`. An empty trailing line in TAGS is safely skipped by `xargs -I {}`.
- Quoting: `echo "${TAGS}"` preserves newlines; `"${DIGEST}"` should be quoted to avoid word-splitting if the digest contains special characters. Cosign's `--yes` flag suppresses interactive prompts (needed in CI).

## Core concepts

**Cosign auto-detection in GitHub Actions:**

When cosign detects it is running in a GitHub Actions environment (presence of `ACTIONS_ID_TOKEN_REQUEST_URL` and `ACTIONS_ID_TOKEN_REQUEST_TOKEN`), it internally calls the OIDC endpoint with the correct `Authorization: Bearer <token>` header and requests the `sigstore` audience. This is the same flow the broken script attempted but failed to authenticate.

**Before (broken):**
```yaml
- name: Retrieve OIDC token        # ← unnecessary, broken
  id: auth
  ...
- name: Sign the published Docker image
  run: echo "${TAGS}" | xargs -I {} cosign sign --yes --identity-token ${{ steps.auth.outputs.oidc }} {}@${DIGEST}
```

**After (fixed):**
```yaml
- name: Sign the published Docker image
  run: echo "${TAGS}" | xargs -I {} cosign sign --yes {}@${DIGEST}
```

## Sub-Tasks

### Sub-Task 1: Remove the "Retrieve OIDC token" step

- **Status:** Pending
- **Objective:** Delete lines 101–113 (the entire `Retrieve OIDC token` step) from the workflow.
- **Related Requirements:** R1, R2
- **Dependencies and Preconditions:** None — the workflow file exists and is checked out.
- **In Scope for This Sub-Task:**
  - Remove the YAML block starting at `- name: Retrieve OIDC token` through its `script:` contents.
  - Remove the `id: auth` output that is no longer referenced elsewhere.
- **Out of Scope for This Sub-Task:**
  - Do not modify the sign step yet (Sub-Task 2 handles that).
  - Do not modify any other steps.
- **Instructions:**
  1. Open `.github/workflows/springboot-ci-uam.yml`.
  2. Delete lines 101 through 113 (the entire `Retrieve OIDC token` step).
- **Acceptance Criteria:**
  - The `Retrieve OIDC token` step no longer appears in the workflow.
  - No remaining reference to `steps.auth.outputs.oidc` exists (will be caught in Sub-Task 2).
- **Cautionary Points (Risks & Edge Cases):**
  - Ensure the indentation of the remaining steps is not broken by the removal.
  - The step list uses 8-space indent under `steps:`.
- **Implementation Suggestions:**
  - Use `edit` tool with exact `oldString` covering lines 101–113.
- **Testing Suggestions:**
  - After removal, run `git diff` to verify only the intended lines are removed.
- **Done When:**
  - The workflow file no longer contains the `Retrieve OIDC token` step.

### Sub-Task 2: Simplify the "Sign the published Docker image" step

- **Status:** Pending
- **Objective:** Update the `run:` command to drop `--identity-token ${{ steps.auth.outputs.oidc }}` since cosign will auto-detect the OIDC environment.
- **Related Requirements:** R1, R2, R3
- **Dependencies and Preconditions:** Sub-Task 1 must be complete (the `steps.auth.outputs.oidc` reference must be removed).
- **In Scope for This Sub-Task:**
  - Change the `run:` value from:
    ```yaml
    run: echo "${TAGS}" | xargs -I {} cosign sign --yes --identity-token ${{ steps.auth.outputs.oidc }} {}@${DIGEST}
    ```
    to:
    ```yaml
    run: echo "${TAGS}" | xargs -I {} cosign sign --yes {}@${DIGEST}
    ```
- **Out of Scope for This Sub-Task:**
  - Do not modify any other part of the workflow.
- **Instructions:**
  1. Open `.github/workflows/springboot-ci-uam.yml`.
  2. Replace the `run:` line in the `Sign the published Docker image` step.
- **Acceptance Criteria:**
  - The `run:` command no longer references `--identity-token` or `steps.auth.outputs.oidc`.
  - The `env:` block (`TAGS`, `DIGEST`) and `if:` guard remain unchanged.
- **Cautionary Points (Risks & Edge Cases):**
  - The `env:` block variables (`TAGS`, `DIGEST`) remain defined — they are still needed by the `run:` command.
  - Validate that no other step references `steps.auth.outputs.oidc` (should be none after Sub-Task 1).
- **Implementation Suggestions:**
  - Use `edit` tool with the exact `oldString` matching the current line 119.
- **Testing Suggestions:**
  - Run `git diff` to confirm only the intended change.
  - Optionally validate YAML syntax: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/springboot-ci-uam.yml'))"`.
- **Done When:**
  - The `run:` command in the sign step no longer contains `--identity-token`.
  - No reference to `steps.auth.outputs.oidc` exists anywhere in the file.

## Final Integration & Verification

- **System-Wide Test:**
  1. Commit the changes and push to a `feature/*` branch.
  2. Create a PR against `develop` and verify the `docker-publish` job is skipped correctly (PR event → sign step skipped via `if:`).
  3. Merge to `develop` and verify the full pipeline runs: build → docker build/push → cosign sign succeeds.
- **Completion Checklist:**
  - [ ] Sub-Task 1 complete — `Retrieve OIDC token` step removed.
  - [ ] Sub-Task 2 complete — `--identity-token` removed from sign command.
  - [ ] `git diff` shows only the two intended changes.
  - [ ] YAML syntax is valid.
  - [ ] No stale references to `steps.auth.outputs.oidc` remain.

## Open Questions

- None. Option B is well understood and self-contained.
