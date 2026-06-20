# Code Review Summary

**Scope**: Staged changes (11 files) — new `GET /uam/v1/users/{userId}` endpoint, `E03ResourceNotFound` error, `ResourceEnum` refactor
**Overall risk**: Medium
**Verdict**: Request changes

---

## Findings

### [P1] Security: Password hash leaked in user API response

- **Location**:
  - `app/uam/src/main/java/com/anasdidi/uam/dto/model/UserDTO.java:23`
  - `app/uam/src/main/java/com/anasdidi/uam/service/impl/GetUserService.java:308`
  - `app/uam/src/main/java/com/anasdidi/uam/service/impl/SearchUserService.java:56` (pre-existing instance of same pattern)
- **Why it matters**: `UserDTO` includes a `password` field and `objectMapper.convertValue(result, UserDTO.class)` copies the entity's password hash directly into the API response. The `GET /uam/v1/users/{userId}` endpoint (and the existing search endpoint) will expose `password` in the JSON response body. The app has no authentication (`spring-boot-starter-security` is commented out), so any caller can retrieve password hashes.
- **Evidence**: `UserDTO.java:23` declares `private String password;` (no `@JsonIgnore`). `UserEntity.java:29-30` has `@Column(name = "PWD") private String password;`. `objectMapper.convertValue` copies all matching fields by name. The same issue exists in `SearchUserService` (pre-existing).
- **Fix**: Add `@JsonIgnore` to `UserDTO.password`. If the password is never needed in API responses, remove the field from `UserDTO` entirely. The `RegisterUserResDTO` does not include it — the get/search endpoints should follow the same pattern.

### [P2] `orElseThrow` lambda throws instead of returning the exception

- **Location**: `app/uam/src/main/java/com/anasdidi/uam/service/impl/GetUserService.java:302-305`
  ```java
  var result = userRepository.findById(req.getPayload().getUserId()).orElseThrow(() -> {
    log.error("User ID not found! {}", req.getPayload().getUserId());
    throw new E03ResourceNotFound(ResourceEnum.USER);     // ← throws, should return
  });
  ```
- **Why it matters**: `orElseThrow` expects a `Supplier<? extends Throwable>` whose `get()` returns the exception. Throwing inside the lambda works incidentally (the thrown exception propagates), but is semantically incorrect, confusing to maintainers, and will be flagged by static analysis tools (e.g., `throw inside a supplier` warnings).
- **Evidence**: The JavaDoc for `Optional.orElseThrow` states: *"If a value is not present, throws the exception produced by the exception supplying function."* The supplier should *produce* (return), not *throw*.
- **Fix**: Replace `throw` with `return`:
  ```java
  var result = userRepository.findById(req.getPayload().getUserId()).orElseThrow(() -> {
    log.error("User ID not found! {}", req.getPayload().getUserId());
    return new E03ResourceNotFound(ResourceEnum.USER);
  });
  ```

### [P2] Missing tests for `GetUserService`

- **Location**: No test file exists for `GetUserService`
- **Why it matters**: JaCoCo enforces ≥80% line coverage for `com.anasdidi.uam.service.impl` during `verify`. A new service class without tests risks lowering coverage below the threshold, causing build failures. Additionally, the new endpoint logic (entity lookup, not-found error, DTO mapping) has no automated validation.
- **Evidence**: `app/uam/src/test/java/com/anasdidi/uam/service/impl/` contains `RegisterUserServiceTests.java` (314 lines) but nothing for `GetUserService`. The POM binds JaCoCo to `verify`.
- **Fix**: Add `GetUserServiceTests` covering at minimum:
  - `testGetUser_success` — valid UUID, found → S00_SUCCESS + payload
  - `testGetUser_notFound` — valid UUID, not found → E03_RESOURCE_NOT_FOUND + null payload
  - `testGetUser_nullPayload` — null payload → E01_VALIDATION_ERROR
  - `testGetUser_missingCorrelationId` — empty correlationId → E01_VALIDATION_ERROR
  Follow the exact pattern from `RegisterUserServiceTests` (`@SpringBootTest`, `@Transactional`, assertion patterns).

### [P3] `E03_RESOURCE_NOT_FOUND` uses HTTP 400 instead of 404

- **Location**: `app/common/src/main/java/com/anasdidi/common/enums/ResponseEnum.java:48`
  ```java
  E03_RESOURCE_NOT_FOUND(HttpStatus.BAD_REQUEST, "E03", "%s Not Found"),
  ```
- **Why it matters**: A "resource not found" condition is semantically HTTP 404 Not Found. Returning 400 Bad Request would mislead API clients and break REST conventions. However, this finding is downgraded because it follows the same convention as `E02_RESOURCE_ALREADY_EXISTS` (also uses 400 instead of the more appropriate 409 Conflict) — it is consistent with the existing error pattern, not a regression.
- **Evidence**: `UserControllerV1.java:178` returns `ResponseEntity.status(res.getResponse().httpStatus).body(res)`, so the 400 propagates to the HTTP response.
- **Fix** (deferrable): Change to `HttpStatus.NOT_FOUND`. If this is a deliberate project-wide convention (all errors use 4xx codes mapped to internal error codes), consider documenting the rationale. Recommend revisiting across all `ResponseEnum` entries as a follow-up task.

---

## Suggested Next Steps

- [ ] Fix P1: Add `@JsonIgnore` to `UserDTO.password` (or remove the field)
- [ ] Fix P2: Correct `throw` → `return` in `GetUserService.orElseThrow`
- [ ] Fix P2: Add `GetUserServiceTests` with the cases listed above
- [ ] Run `./mvnw spotless:apply && ./mvnw verify -pl uam -am` to confirm formatting + all tests + JaCoCo coverage
- [ ] Consider reviewing `SearchUserService` for the same password leak (pre-existing, but worth fixing in the same pass)
