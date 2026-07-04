# Plan: Add Missing Tests & Minor Fixes for Delete User Feature

## Objective

Add integration and controller tests for the staged Delete User feature, along with minor consistency fixes, to ensure the full pipeline (`verify`) passes with ≥80% JaCoCo coverage.

## Requirements Snapshot

- **R1 (Delete Service — Integration Tests):** Using `@SpringBootTest` with in-memory H2, cover success (soft-delete sets `isDeleted=true`), not-found (E03), version-mismatch (E03), and validation error (E01) scenarios for `DeleteUserService`.
- **R2 (Delete Controller — Web MVC Tests):** Using `MockMvcBuilders.standaloneSetup()` with mocked `DeleteUserService`, cover success (204 No Content), missing correlationId (400), missing version param (400), E03 → 404, E99 → 500.
- **R3 (Consistency):** Add `@Slf4j` to `DeleteUserService` to match the pattern of all other services (`UpdateUserService`, `RegisterUserService`, `GetUserService`, `SearchUserService`).
- **R4 (Coverage):** JaCoCo 80% line coverage for `com.anasdidi.uam.service.impl` and `com.anasdidi.uam.controller.impl` must be maintained / met.

## Scope

- Create `DeleteUserServiceTests.java` — ~5 test methods covering success, not-found, version-mismatch, null-payload, and missing-correlationId.
- Add `deleteUser` test methods to existing `UserControllerV1Tests.java` — ~5 test methods.
- Add `@Slf4j` annotation to `DeleteUserService`.
- Run `spotless:apply` and `verify` to confirm formatting and coverage.

## Assumptions and Constraints

- All commands run from `app/` using the canonical `./mvnw`.
- Existing tests for RegisterUser, GetUser, SearchUser, UpdateUser pass and must not be broken.
- `UserControllerV1Tests` already uses `@ExtendWith(MockitoExtension.class)` with `MockMvcBuilders.standaloneSetup()` — new tests follow the same pattern.
- `DeleteUserReqDTO` payload has `@NotNull UUID userId`, `@NotNull Integer version`.
- `DeleteUserService` returns `S02_DELETED` (HTTP 204 No Content) with no payload on success.
- JaCoCo runs during `verify` only; `test` alone does not enforce coverage.
- The `UserRepository.findByIdAndVersion(UUID, Integer)` method already exists.
- `DeleteUserResDTO` has no payload field — response body on success will only have base fields (correlationId, traceId, timestamp, timeTaken, responseCode, responseDesc).

## Risks and Areas Requiring Care

- `S02_DELETED.httpStatus` is `HttpStatus.NO_CONTENT` (204) — controller test must assert `isNoContent()` not `isOk()`.
- HTTP 204 No Content typically omits a response body — but the controller calls `ResponseEntity.status(res.getResponse().httpStatus).body(res)` which will still serialize the DTO. The test should verify the JSON structure is present despite the 204 status. This is an intentional pattern consistency choice in this codebase.
- `findByIdAndVersion` returns `Optional.empty()` for both non-existent id AND wrong version — both map to `E03_RESOURCE_NOT_FOUND` with the same error map.
- `DeleteUserReqDTO` is a nested-payload DTO — service test must build it correctly with `.payload(DeleteUserReqDTOPayload.builder()...)`.
- The `DeleteMapping` uses `@RequestParam Integer version` (query param) — controller tests must pass it correctly via `.param("version", "1")`.
- The mock for `deleteUser` controller tests needs `any(DeleteUserReqDTO.class)` — use fully qualified or imported type.

## Sub-Tasks

### Sub-Task 1: Add `@Slf4j` to DeleteUserService

- **Status:** Pending
- **Objective:** Add `@Slf4j` Lombok annotation to `DeleteUserService` for consistency with all other services.
- **Related Requirements:** R3
- **Dependencies and Preconditions:** None
- **In Scope for This Sub-Task:**
  - Add `import lombok.extern.slf4j.Slf4j;` to `DeleteUserService.java`.
  - Add `@Slf4j` annotation to the class.
- **Out of Scope for This Sub-Task:** Any behavioral changes or test additions.
- **Instructions:**
  1. Read `DeleteUserService.java`.
  2. Add `import lombok.extern.slf4j.Slf4j;` to imports.
  3. Add `@Slf4j` annotation above the `@Service` line.
  4. Run `./mvnw spotless:apply -pl uam -am` to format.
- **Acceptance Criteria:**
  - `DeleteUserService` has `@Slf4j` annotation.
  - Compilation passes.
- **Cautionary Points (Risks & Edge Cases):** None — pure annotation addition.
- **Implementation Suggestions:** Place `@Slf4j` on its own line before `@Service`.
- **Testing Suggestions:** Run `./mvnw compile -pl uam -am` to confirm compilation.
- **Done When:** Change is applied and `./mvnw compile -pl uam -am` passes.

### Sub-Task 2: Create DeleteUserServiceTests

- **Status:** Pending
- **Objective:** Create integration tests for `DeleteUserService` covering success, not-found, version-mismatch, and validation error scenarios.
- **Related Requirements:** R1, R4
- **Dependencies and Preconditions:** Sub-Task 1 is not required for test correctness but can be done in any order.
- **In Scope for This Sub-Task:**
  - Create `src/test/java/com/anasdidi/uam/service/impl/DeleteUserServiceTests.java`.
  - Test methods:
    1. `testDeleteUser_success` — seed user, delete, assert `S02_DELETED` + `"02"` + `"Deleted"`, assert entity `isDeleted=true` in DB.
    2. `testDeleteUser_notFound` — use non-existent UUID, assert `E03_RESOURCE_NOT_FOUND`, null payload.
    3. `testDeleteUser_versionMismatch` — seed user, call with wrong version, assert `E03_RESOURCE_NOT_FOUND`.
    4. `testDeleteUser_nullPayload` — use `ObjenesisStd` to set `payload = null`, assert `E01_VALIDATION_ERROR`.
    5. `testDeleteUser_missingCorrelationId` — empty correlationId, assert `E01_VALIDATION_ERROR`.
- **Out of Scope for This Sub-Task:** Controller tests, `@Slf4j` fix.
- **Instructions:**
  - Follow the pattern in `UpdateUserServiceTests.java`:
    - `@SpringBootTest` on the class.
    - `@Transactional` on each test method.
    - `@Autowired DeleteUserService`, `@Autowired UserRepository`.
    - Use `ObjenesisStd` + `ReflectionTestUtils` for null-payload tests.
  - For the success test, verify:
    - `ResponseEnum.S02_DELETED`, `"02"`, `"Deleted"`.
    - `getPayload()` is null (no payload in `DeleteUserResDTO`).
    - Fetch entity from DB and assert `.getIsDeleted()` is `true`.
  - Use a `seedUser(String username, String name)` helper method (same pattern as `UpdateUserServiceTests`).
  - For the null-payload test:
    ```java
    var objenesis = new ObjenesisStd();
    var req = objenesis.newInstance(DeleteUserReqDTO.class);
    ReflectionTestUtils.setField(req, "correlationId", "corr-null");
    ReflectionTestUtils.setField(req, "payload", null);
    ```
  - For the missing-correlationId test, use `.correlationId("")` in the builder.
- **Acceptance Criteria:**
  - All 5 tests pass with `./mvnw test -pl uam -am -Dtest=DeleteUserServiceTests`.
- **Cautionary Points (Risks & Edge Cases):**
  - `DeleteUserReqDTO` builder path: `DeleteUserReqDTO.builder().correlationId("...").payload(DeleteUserReqDTOPayload.builder().userId(...).version(...).build()).build()`.
  - `DeleteUserResDTO` has no payload field — `getPayload()` always returns null, even on success. Do not assert payload in success case.
  - `@NotNull` on `userId` and `version` in payload — the null-payload test bypasses this via Objenesis.
- **Implementation Suggestions:**
  - Helper method for seeding:
    ```java
    private UserEntity seedUser(String username, String name) {
      return userRepository.save(UserEntity.builder()
          .username(username)
          .password("pass")
          .name(name)
          .isDeleted(false)
          .build());
    }
    ```
- **Testing Suggestions:** Run `./mvnw test -pl uam -am -Dtest=DeleteUserServiceTests`.
- **Done When:** All 5 tests pass and `spotless:apply` has been run.

### Sub-Task 3: Add deleteUser controller tests to UserControllerV1Tests

- **Status:** Pending
- **Objective:** Add Web MVC controller tests for the `DELETE /uam/v1/user/{userId}?version=` endpoint covering success, error, and validation scenarios.
- **Related Requirements:** R2, R4
- **Dependencies and Preconditions:** None. Can be done in parallel with Sub-Task 2.
- **In Scope for This Sub-Task:**
  - Add to existing `UserControllerV1Tests.java`:
    1. Mock field: `@Mock private DeleteUserService deleteUserService;`
    2. Add import for `import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;`
    3. Add import for `import com.anasdidi.uam.dto.DeleteUserResDTO;` (for mock response builder)
    4. Add import for `import com.anasdidi.uam.service.impl.DeleteUserService;`
    5. Test methods:
       - `testDeleteUser_success` — mock `S02_DELETED`, assert 204 No Content + jsonPath for base fields.
       - `testDeleteUser_missingCorrelationId` — no header, assert 400 Bad Request.
       - `testDeleteUser_missingVersion` — no `version` param, assert 400 Bad Request (Spring rejects before controller).
       - `testDeleteUser_serviceReturnsE03` — mock `E03_RESOURCE_NOT_FOUND`, assert 404 + null payload.
       - `testDeleteUser_serviceReturnsE99` — mock `E99_UNEXPECTED_ERROR`, assert 500.
- **Out of Scope for This Sub-Task:** Service tests (Sub-Task 2), `@Slf4j` fix.
- **Instructions:**
  - Follow the pattern in existing test methods within the same file:
    - `@Mock private DeleteUserService deleteUserService;` field alongside other mocks.
    - Inject via `@InjectMocks` (already present — `UserControllerV1` constructor takes all services).
    - Use `MockMvcRequestBuilders.delete(...)` for the DELETE call.
    - URL: `BASE_URL + "/{userId}"` with `.param("version", "1")` for the query param.
    - Header: `.header(CommonConstants.HEADER_CORR_ID, CORRELATION_ID)`.
  - For the success test:
    ```java
    var userId = UUID.randomUUID();
    var mockResponse = DeleteUserResDTO.builder()
        .correlationId(CORRELATION_ID)
        .response(ResponseEnum.S02_DELETED)
        .build();
    when(deleteUserService.execute(any(DeleteUserReqDTO.class))).thenReturn(mockResponse);
    mockMvc
        .perform(delete(BASE_URL + "/" + userId)
            .header(CommonConstants.HEADER_CORR_ID, CORRELATION_ID)
            .param("version", "1"))
        .andExpect(status().isNoContent())
        .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID));
    ```
  - For the E03 test:
    ```java
    var mockResponse = DeleteUserResDTO.builder()
        .correlationId(CORRELATION_ID)
        .response(ResponseEnum.E03_RESOURCE_NOT_FOUND)
        .build();
    when(deleteUserService.execute(any(DeleteUserReqDTO.class))).thenReturn(mockResponse);
    mockMvc
        .perform(delete(BASE_URL + "/" + userId)
            .header(CommonConstants.HEADER_CORR_ID, CORRELATION_ID)
            .param("version", "1"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID))
        .andExpect(jsonPath("$.payload").doesNotExist());
    ```
- **Acceptance Criteria:**
  - All 5 new controller tests pass alongside all existing tests.
- **Cautionary Points (Risks & Edge Cases):**
  - `@RequestParam Integer version` defaults to `required=true` so missing param → 400 from Spring before controller is invoked.
  - `ResponseEntity.status(HttpStatus.NO_CONTENT).body(res)` still serializes the body despite 204. The test should verify body content exists — this is the codebase's chosen behavior.
  - `MockMvcRequestBuilders.delete(...)` needs `import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;`.
- **Implementation Suggestions:**
  - Add these imports at the top of the file:
    ```java
    import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
    import com.anasdidi.uam.dto.DeleteUserResDTO;
    import com.anasdidi.uam.service.impl.DeleteUserService;
    ```
  - Use `UUID.randomUUID()` for userId in all test methods.
- **Testing Suggestions:**
  - Run `./mvnw test -pl uam -am -Dtest=UserControllerV1Tests` to verify new controller tests.
  - Then run the full set: `./mvnw test -pl uam -am -Dtest=UserControllerV1Tests,DeleteUserServiceTests`.
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
  - [ ] `@Slf4j` added to `DeleteUserService`
  - [ ] `DeleteUserServiceTests` created with 5 test methods — all pass
  - [ ] `UserControllerV1Tests` has 5 new `testDeleteUser_*` methods — all pass
  - [ ] All existing tests still pass
  - [ ] `spotless:apply` run, no formatting violations
  - [ ] `./mvnw verify -pl uam -am` passes

## Open Questions

- None.
