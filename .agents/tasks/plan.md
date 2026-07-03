# Plan: Fix Uppercase Inconsistency & Add Missing Tests for Update User

## Objective

Fix the name-uppercase inconsistency in `UpdateUserService`, then add comprehensive service (integration) and controller (Web MVC) tests for the update user flow. Ensure 80% line coverage is sustained.

## Requirements Snapshot

- **R1 (Uppercase Fix):** `UpdateUserService` must uppercase the name before persisting, matching `RegisterUserService` behavior.
- **R2 (Service Tests — Integration):** Using `@SpringBootTest` with in-memory H2, cover success, not-found (E03), and validation error (E01) scenarios.
- **R3 (Controller Tests — Web MVC):** Using `MockMvcBuilders.standaloneSetup()` with mocked services, cover success, missing headers/params/body, and service-error HTTP status mappings.
- **R4 (Coverage):** JaCoCo 80% line coverage for `com.anasdidi.uam.service.impl` and `com.anasdidi.uam.controller.impl` must be maintained.

## Scope

- Fix `UpdateUserService.java` — add `.toUpperCase()` for name.
- Create `UpdateUserServiceTests.java` — ~8 test methods.
- Add `updateUser` test methods to existing `UserControllerV1Tests.java` — ~6 test methods.
- Run `spotless:apply` and `verify` to confirm formatting and coverage.

## Assumptions and Constraints

- All commands run from `app/` using the canonical `./mvnw`.
- Existing tests for RegisterUser/GetUser/SearchUser pass and must not be broken.
- `UserControllerV1Tests` already uses `@ExtendWith(MockitoExtension.class)` with `MockMvcBuilders.standaloneSetup()` — new tests follow the same pattern.
- `UpdateUserReqDTO` has `@NotNull UUID userId`, `@NotNull Integer version`, `@Valid @NotNull UpdateUserReqDTOPayload payload` (with `@NotBlank String name`).
- JaCoCo runs during `verify` only; `test` alone does not enforce coverage.

## Risks and Areas Requiring Care

- Adding `.toUpperCase()` is a one-line change but could affect existing stored data semantics (none exist in test H2).
- The `E03_RESOURCE_NOT_FOUND` message is `"%s Not Found"` → formatted as `"User Not Found"` — verify responseDesc assertion.
- The `PatchMapping` uses `@RequestParam Integer version` (query param) not path variable — controller tests must pass it correctly.

## Sub-Tasks

### Sub-Task 1: Fix name uppercase in UpdateUserService

- **Status:** Pending
- **Objective:** Make `UpdateUserService.execute()` uppercase the user's name before saving, matching `RegisterUserService`.
- **Related Requirements:** R1
- **Dependencies and Preconditions:** None
- **In Scope for This Sub-Task:** Edit `UpdateUserService.java` line 37: add `.toUpperCase()` to `req.getPayload().getName()`.
- **Out of Scope for This Sub-Task:** Any other behavioral changes, refactoring, or test additions.
- **Instructions:**
  1. Read `UpdateUserService.java`.
  2. Change `entity.setName(req.getPayload().getName())` → `entity.setName(req.getPayload().getName().toUpperCase())`.
  3. Run `./mvnw spotless:apply -pl uam` to format.
- **Acceptance Criteria:**
  - `UpdateUserService` uppercases name before `userRepository.save(entity)`.
- **Cautionary Points (Risks & Edge Cases):**
  - Already-uppercased names are harmless idempotent calls.
  - Lombok `@Slf4j` and other annotations are unaffected.
- **Implementation Suggestions:** No new imports needed; `String.toUpperCase()` is JDK built-in.
- **Testing Suggestions:** Run `./mvnw compile -pl uam -am` to confirm compilation passes.
- **Done When:** The change is committed and `./mvnw compile -pl uam -am` passes.

### Sub-Task 2: Add UpdateUserServiceTests

- **Status:** Pending
- **Objective:** Create integration tests for `UpdateUserService` covering success, not-found, and validation error scenarios.
- **Related Requirements:** R2, R4
- **Dependencies and Preconditions:** Sub-Task 1 (uppercase fix) must be completed first so tests assert uppercased name.
- **In Scope for This Sub-Task:**
  - Create `src/test/java/com/anasdidi/uam/service/impl/UpdateUserServiceTests.java`.
  - Test methods:
    1. `testUpdateUser_success` — seed user, update name, assert S00_SUCCESS + userId + name stored uppercased.
    2. `testUpdateUser_notFound` — use non-existent userId+version, assert E03_RESOURCE_NOT_FOUND, null payload.
    3. `testUpdateUser_versionMismatch` — seed user, call with wrong version, assert E03_RESOURCE_NOT_FOUND.
    4. `testUpdateUser_nullPayload` — use `ObjenesisStd` to set `payload = null`, assert E01_VALIDATION_ERROR.
    5. `testUpdateUser_missingCorrelationId` — empty correlationId, assert E01_VALIDATION_ERROR.
    6. `testUpdateUser_nullName` — `name = null` in payload, assert E01_VALIDATION_ERROR.
    7. `testUpdateUser_blankName` — `name = ""` in payload, assert E01_VALIDATION_ERROR.
    8. `testUpdateUser_nameUppercased` — update with mixed-case name, assert entity name in DB is uppercased.
- **Out of Scope for This Sub-Task:** Controller tests.
- **Instructions:**
  - Follow the pattern in `RegisterUserServiceTests` / `GetUserServiceTests`:
    - `@SpringBootTest`, `@Transactional` on each test method.
    - `@Autowired UpdateUserService`, `@Autowired UserRepository`.
    - Use `ObjenesisStd` + `ReflectionTestUtils` for null-payload tests.
  - For the success test, verify:
    - `ResponseEnum.S00_SUCCESS`, `"00"`, `"Success"`.
    - `getPayload()` is not null and contains `userId`.
    - Fetch entity from DB and assert `.getName()` is uppercased.
  - For the nameUppercased test, seed with original name `"John Doe"`, update with `"Jane smith"`, assert entity name in DB is `"JANE SMITH"`.
- **Acceptance Criteria:**
  - All 8 tests pass with `./mvnw test -pl uam -am -Dtest=UpdateUserServiceTests`.
- **Cautionary Points (Risks & Edge Cases):**
  - The `findByIdAndVersion` returns `Optional.empty()` for both non-existent id AND wrong version — both map to `E03_RESOURCE_NOT_FOUND`.
  - `@NotBlank` rejects `null`, `""`, and `"   "` — one test using `""` covers this group since the aspect translates `ConstraintViolationException` → `E01_VALIDATION_ERROR`.
- **Implementation Suggestions:**
  - Helper method to seed a user (similar to `SearchUserServiceTests.seedUser()`).
  - Helper method to build a valid `UpdateUserReqDTO`.
- **Testing Suggestions:** Run `./mvnw test -pl uam -am -Dtest=UpdateUserServiceTests`.
- **Done When:** All 8 tests pass and `spotless:apply` has been run.

### Sub-Task 3: Add updateUser controller tests to UserControllerV1Tests

- **Status:** Pending
- **Objective:** Add Web MVC controller tests for the PATCH `/user/{userId}` endpoint covering success, error, and validation scenarios.
- **Related Requirements:** R3, R4
- **Dependencies and Preconditions:** Sub-Task 1 (uppercase fix). Sub-Task 2 can be done in parallel but order does not matter.
- **In Scope for This Sub-Task:**
  - Add to existing `UserControllerV1Tests.java`:
    1. Mock field for `UpdateUserService`.
    2. Test methods:
       - `testUpdateUser_success` — mock S00_SUCCESS with payload, assert 200 OK + jsonPath assertions.
       - `testUpdateUser_missingCorrelationId` — no header, assert 400 Bad Request.
       - `testUpdateUser_missingVersion` — no `version` query param, assert 400 Bad Request.
       - `testUpdateUser_serviceReturnsE03` — mock E03_RESOURCE_NOT_FOUND, assert 404 + null payload.
       - `testUpdateUser_serviceReturnsE99` — mock E99_UNEXPECTED_ERROR, assert 500.
       - `testUpdateUser_emptyBody` — no request body, assert 400 Bad Request.
- **Out of Scope for This Sub-Task:** Service tests (Sub-Task 2).
- **Instructions:**
  - Follow the pattern in existing test methods within the same file:
    - `@Mock private UpdateUserService updateUserService;` field.
    - Inject via `@InjectMocks` (already present).
    - Use `MockMvcRequestBuilders.patch(...)` for the PATCH call.
    - URL: `BASE_URL + "/{userId}"` with `.param("version", "1")` for the query param.
    - Request body: `{"name":"Updated Name"}`.
  - Verify HTTP status via `.andExpect(status().isOk())`, `.isNotFound()`, etc.
  - Verify JSON response fields via `.andExpect(jsonPath("$...")...)`.
- **Acceptance Criteria:**
  - All 6 new controller tests pass alongside all existing tests.
- **Cautionary Points (Risks & Edge Cases):**
  - `@RequestParam Integer version` defaults to `required=true` so missing param → 400 from Spring before controller.
  - The `UpdateUserReqDTO` builder sets `userId` and `version` at top level (not in payload) — controller test must include `.param("version", "1")`.
  - `MockMvcRequestBuilders.patch(...)` requires `import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;` or use fully qualified name. Use the existing import style.
- **Implementation Suggestions:**
  - Add `import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;` at the top.
  - Use `UUID.randomUUID()` for userId in success/error mock tests.
  - Use `MediaType.APPLICATION_JSON` for content type.
- **Testing Suggestions:**
  - Run `./mvnw test -pl uam -am -Dtest=UserControllerV1Tests` to verify new controller tests.
  - Then run the full service test suite alongside: `./mvnw test -pl uam -am -Dtest=UserControllerV1Tests,UpdateUserServiceTests`.
- **Done When:** All tests pass.

### Sub-Task 4: Final formatting & full verify

- **Status:** Pending
- **Objective:** Run `spotless:apply` and `verify` to confirm formatting, all tests pass, and JaCoCo coverage meets ≥80% threshold.
- **Related Requirements:** R4
- **Dependencies and Preconditions:** Sub-Tasks 1, 2, and 3 completed.
- **In Scope for This Sub-Task:**
  1. `./mvnw spotless:apply -pl uam -am` (auto-format all changed files).
  2. `./mvnw verify -pl uam -am` (full pipeline: compile → test → JaCoCo → spotless check).
- **Out of Scope for This Sub-Task:** Code changes beyond formatting.
- **Instructions:**
  - Run commands in order: `spotless:apply` first, then `verify`.
  - If `verify` fails on spotless check, run `spotless:apply` again and re-verify.
  - If `verify` fails on JaCoCo coverage, review test coverage and add missing test scenarios.
- **Acceptance Criteria:**
  - `./mvnw verify -pl uam -am` exits with `BUILD SUCCESS`.
  - JaCoCo report shows ≥80% for both `com.anasdidi.uam.service.impl` and `com.anasdidi.uam.controller.impl`.
- **Cautionary Points (Risks & Edge Cases):**
  - `spotless:check` is bound to `verify` — must run `spotless:apply` first.
  - JaCoCo thresholds apply only to `verify`, not `test`.
- **Testing Suggestions:** N/A — this is the final validation step.
- **Done When:** `BUILD SUCCESS` from `./mvnw verify -pl uam -am`.

## Final Integration & Verification

- **System-Wide Test:** `./mvnw verify -pl uam -am` passes with ≥80% JaCoCo coverage.
- **Completion Checklist:**
  - [ ] `UpdateUserService` uppercases name
  - [ ] `UpdateUserServiceTests` created with 8 test methods — all pass
  - [ ] `UserControllerV1Tests` has 6 new `testUpdateUser_*` methods — all pass
  - [ ] All existing tests still pass
  - [ ] `spotless:apply` run, no formatting violations
  - [ ] `./mvnw verify -pl uam -am` passes

## Open Questions

- None.
