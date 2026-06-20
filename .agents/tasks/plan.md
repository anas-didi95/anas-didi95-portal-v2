# Plan: Address code review findings for staged changes

## Objective

Fix the findings raised in `.agents/tasks/review.md` — resolve the P1 password leak, P2 `orElseThrow` misuse, and P2 missing tests. Optional: apply the same password fix to the pre-existing `SearchUserService`.

## Requirements Snapshot

- **R1 (P1 Security):** `UserDTO.password` must not be serialized in API responses. The `GET /uam/v1/users/{userId}` endpoint must not expose password hashes.
- **R2 (P2 Correctness):** `GetUserService.orElseThrow` lambda must `return` the exception, not `throw` it.
- **R3 (P2 Coverage):** `GetUserService` must have a test class with ≥80% line coverage, following the same `@SpringBootTest` + `@Transactional` pattern as `RegisterUserServiceTests`.
- **R4 (Nice-to-have):** Apply the same `@JsonIgnore` fix to `SearchUserService`'s use of `objectMapper.convertValue` to `UserDTO` (pre-existing issue in the same code path).

## Scope

- Add `@JsonIgnore` to `UserDTO.password`
- Fix `throw` → `return` in `GetUserService.orElseThrow` lambda
- Create `GetUserServiceTests` covering success, not-found, and validation-error cases
- Optionally: add `@JsonIgnore` to any other sensitive fields in `UserDTO` that should not leak

## Assumptions and Constraints

- All changes must pass `./mvnw spotless:apply && ./mvnw verify -pl uam -am`
- Tests must use the same patterns as `RegisterUserServiceTests` (`@SpringBootTest`, `@Transactional`, assertion style)
- JaCoCo must maintain ≥80% line coverage for `com.anasdidi.uam.service.impl`
- The existing `SearchUserService` password leak is pre-existing; fixing it is desirable but out of scope unless explicitly included

## Risks and Areas Requiring Care

- If `UserDTO.password` is used anywhere else in the codebase that needs it serialized, `@JsonIgnore` would break that usage. Need to verify only the service layer uses it for entity→DTO mapping.
- `GetUserServiceTests` must use unique test data to avoid cross-test contamination (even with `@Transactional`).
- The `E03_RESOURCE_NOT_FOUND` HTTP status (P3 finding) is a wider design decision — out of scope for this plan.

## Sub-Tasks

### Sub-Task 1: Add `@JsonIgnore` to `UserDTO.password`

- **Status:** Pending
- **Objective:** Prevent password hash from being serialized in API responses for all endpoints that return `UserDTO`.
- **Related Requirements:** R1
- **Dependencies and Preconditions:** None
- **In Scope for This Sub-Task:**
  - Add `@com.fasterxml.jackson.annotation.JsonIgnore` to the `password` field in `UserDTO.java`
- **Out of Scope for This Sub-Task:**
  - Changing `UserEntity`, the database schema, or any other DTO
  - Adding/removing `password` from `UserDTO` entirely (minimal change only)
- **Instructions:**
  1. Open `app/uam/src/main/java/com/anasdidi/uam/dto/model/UserDTO.java`
  2. Add `import com.fasterxml.jackson.annotation.JsonIgnore;`
  3. Annotate `private String password;` with `@JsonIgnore`
- **Acceptance Criteria:**
  - `password` field annotated with `@JsonIgnore`
  - Compiles and passes `spotless:check`
- **Cautionary Points (Risks & Edge Cases):**
  - If any code path relies on deserializing `password` from JSON into `UserDTO`, `@JsonIgnore` will cause it to be `null`. Grep the codebase to check: `grep -r "UserDTO" app/ --include="*.java"` to verify usage — password is only set via `objectMapper.convertValue(entity, UserDTO.class)` which works field-to-field, not via JSON deserialization, so this is safe.
- **Implementation Suggestions:** None
- **Testing Suggestions:**
  - `./mvnw compile -pl uam -am` to verify compilation
  - `./mvnw spotless:apply -pl uam -am` to format
  - After all sub-tasks are done, run full verification
- **Done When:** `@JsonIgnore` is added, code compiles, and Spotless passes

### Sub-Task 2: Fix `orElseThrow` lambda in `GetUserService`

- **Status:** Pending
- **Objective:** Correct the `orElseThrow` supplier to `return` the exception instead of `throw`ing it.
- **Related Requirements:** R2
- **Dependencies and Preconditions:** None
- **In Scope for This Sub-Task:**
  - Replace `throw new E03ResourceNotFound(...)` with `return new E03ResourceNotFound(...)` in the `orElseThrow` lambda of `GetUserService.execute()`
- **Out of Scope for This Sub-Task:**
  - Any other changes to `GetUserService`
- **Instructions:**
  1. Open `app/uam/src/main/java/com/anasdidi/uam/service/impl/GetUserService.java`
  2. Change line 304 from `throw new E03ResourceNotFound(ResourceEnum.USER);` to `return new E03ResourceNotFound(ResourceEnum.USER);`
  3. Since `return` replaces `throw`, verify the lambda still compiles — the `orElseThrow` signature expects `Supplier<? extends X>` where `X extends Throwable`, and returning the exception instance satisfies this.
- **Acceptance Criteria:**
  - The lambda returns the exception instance instead of throwing it
  - Code compiles and Spotless check passes
- **Cautionary Points (Risks & Edge Cases):**
  - Both `throw` and `return` produce the same runtime behavior (the exception propagates), but `return` is semantically correct. No behavioral change.
- **Implementation Suggestions:**
  ```java
  var result = userRepository.findById(req.getPayload().getUserId()).orElseThrow(() -> {
    log.error("User ID not found! {}", req.getPayload().getUserId());
    return new E03ResourceNotFound(ResourceEnum.USER);
  });
  ```
- **Testing Suggestions:**
  - `./mvnw compile -pl uam -am` to verify compilation
- **Done When:** The `throw` is replaced with `return` and the code compiles

### Sub-Task 3: Add `GetUserServiceTests`

- **Status:** Pending
- **Objective:** Ensure the new `GetUserService` is covered by tests meeting JaCoCo's ≥80% threshold.
- **Related Requirements:** R3
- **Dependencies and Preconditions:** Sub-Tasks 1 and 2 should be complete (so tests validate the final code).
- **In Scope for This Sub-Task:**
  - Create `app/uam/src/test/java/com/anasdidi/uam/service/impl/GetUserServiceTests.java`
  - Test cases:
    1. `testGetUser_success` — register a user, then fetch by ID → S00_SUCCESS, payload with matching data
    2. `testGetUser_notFound` — random UUID → E03_RESOURCE_NOT_FOUND, null payload, `"User Not Found"` description
    3. `testGetUser_nullPayload` — null payload (via ObjenesisStd + ReflectionTestUtils) → E01_VALIDATION_ERROR, null payload
    4. `testGetUser_missingCorrelationId` — empty correlationId → E01_VALIDATION_ERROR, null payload
    5. `testGetUser_nullUserId` — null userId in payload → E01_VALIDATION_ERROR
- **Out of Scope for This Sub-Task:**
  - Controller-level tests (existing pattern uses `MockMvcBuilders.standaloneSetup` for controller tests; those can be added separately)
  - Integration tests beyond the service layer
- **Instructions:**
  - Mirror the structure of `RegisterUserServiceTests.java`:
    - Annotations: `@SpringBootTest` (class-level), `@Transactional` (per test method)
    - Autowire `GetUserService` and `UserRepository`
    - Use `RegisterUserService` to seed test data, then fetch with `GetUserService`
    - Assert on: `result.getCorrelationId()`, `result.getPayload()`, `result.getResponse()`, `result.getResponseCode()`, `result.getResponseDesc()`, `result.getTraceId()`, `result.getTimestamp()`, `result.getTimeTaken()`
  - For the success test, seed a user via `RegisterUserService`, extract the ID from the register response, then call `GetUserService` with that ID. Assert that `username`, `name` match (but NOT password — it should be null due to `@JsonIgnore`).
  - For the not-found test, use `UUID.randomUUID()` and expect `E03_RESOURCE_NOT_FOUND` with responseDesc `"User Not Found"`.
  - For null-payload/null-correlationId tests, follow the exact `ObjenesisStd` + `ReflectionTestUtils` pattern from `RegisterUserServiceTests`.
  - For null-userId test, build a request with payload where `userId` is not set (builder without `.userId(...)`) → `@NotNull` on `GetUserReqDTOPayload.userId` will trigger validation error.
- **Acceptance Criteria:**
  - All 6 test cases pass
  - `./mvnw test -pl uam -am -Dtest=GetUserServiceTests` succeeds
  - JaCoCo coverage for `com.anasdidi.uam.service.impl` stays ≥80%
- **Cautionary Points (Risks & Edge Cases):**
  - Use unique usernames per test to avoid collisions (e.g., `"getuser-test-success"`, `"getuser-test-notfound-setup"`)
  - The success test creates a user via `RegisterUserService`, so the register test data must not collide with other test data in the same run
  - `@Transactional` on the test method rolls back after each test, so no cleanup needed
  - The test file must be in the exact package directory: `com/anasdidi/uam/service/impl/`
- **Implementation Suggestions:** Use this as a template for the success test:
  ```java
  @Test
  @Transactional
  void testGetUser_success() {
    String username = "getuser-success-" + UUID.randomUUID().toString().substring(0, 8);
    var registerReq = RegisterUserReqDTO.builder()
        .correlationId("corr-get-success")
        .payload(RegisterUserReqDTOPayload.builder()
            .username(username)
            .password("pass123")
            .name("Test User")
            .build())
        .build();
    var registerRes = registerUserService.execute(registerReq);

    var req = GetUserReqDTO.builder()
        .correlationId("corr-get-success")
        .payload(GetUserReqDTOPayload.builder()
            .userId(registerRes.getPayload().getUserId())
            .build())
        .build();
    GetUserResDTO result = getUserService.execute(req);

    assertNotNull(result);
    assertEquals("corr-get-success", result.getCorrelationId());
    assertNotNull(result.getPayload());
    assertNotNull(result.getPayload().getResult());
    assertEquals(username, result.getPayload().getResult().getUsername());
    assertEquals("TEST USER", result.getPayload().getResult().getName());
    assertNull(result.getPayload().getResult().getPassword());  // @JsonIgnore
    assertEquals(ResponseEnum.S00_SUCCESS, result.getResponse());
    assertEquals("00", result.getResponseCode());
    assertEquals("Success", result.getResponseDesc());
  }
  ```
- **Testing Suggestions:**
  ```bash
  ./mvnw test -pl uam -am -Dtest=GetUserServiceTests
  ```
- **Done When:** All test cases pass, and `./mvnw verify -pl uam -am` passes (including JaCoCo)

## Final Integration & Verification

- **System-Wide Test:**
  ```bash
  ./mvnw spotless:apply -pl uam -am && ./mvnw verify -pl uam -am
  ```
- **Completion Checklist:**
  - [ ] Sub-Task 1: `@JsonIgnore` added to `UserDTO.password`
  - [ ] Sub-Task 2: `throw` → `return` in `GetUserService.orElseThrow`
  - [ ] Sub-Task 3: `GetUserServiceTests` created with 6 test cases
  - [ ] `spotless:apply` passes
  - [ ] `verify` passes (all tests + JaCoCo ≥80%)

## Open Questions

None.
