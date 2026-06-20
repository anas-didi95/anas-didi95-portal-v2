# Code Review Summary

**Scope**: Staged changes — `ResponseEnum.java` (HTTP status corrections + new `S01_CREATED` enum) and `RegisterUserService.java` (return `S01_CREATED` instead of `S00_SUCCESS`)
**Overall risk**: High — tests will fail on `verify`
**Verdict**: Request changes

## Findings

### [P1] High — `RegisterUserServiceTests` assert wrong enum and response code/desc after `S01_CREATED` change

- **Location**: `app/uam/src/test/java/com/anasdidi/uam/service/impl/RegisterUserServiceTests.java:46-51`
- **Why it matters**: `RegisterUserService` now returns `S01_CREATED` (code `"01"`, desc `"Created"`), but the tests still assert `S00_SUCCESS` / `"00"` / `"Success"`. These assertions will fail, breaking `mvnw verify`.
- **Evidence**:
  - `RegisterUserService.java:47` — `return res.response(ResponseEnum.S01_CREATED).payload(payload).build();`
  - Test line 46: `assertEquals(ResponseEnum.S00_SUCCESS, result.getResponse());` ← will get `S01_CREATED`
  - Test line 50: `assertEquals("00", result.getResponseCode());` ← will get `"01"`
  - Test line 51: `assertEquals("Success", result.getResponseDesc());` ← will get `"Created"`
- **Fix**: Update `RegisterUserServiceTests.java` lines 46, 50, 51 to assert `S01_CREATED`, `"01"`, `"Created"`.

- **Location**: `app/uam/src/test/java/com/anasdidi/uam/service/impl/RegisterUserServiceTests.java:307-309`
- **Why it matters**: Same root cause — second test method making the same stale assertions.
- **Evidence**: Lines 307-309 assert `S00_SUCCESS`, `"00"`, `"Success"` but will get `S01_CREATED`, `"01"`, `"Created"`.
- **Fix**: Update same assertions in that test method.

### [P1] High — Controller test asserts `isBadRequest()` for `E02_RESOURCE_ALREADY_EXISTS` but HTTP status changed to `CONFLICT`

- **Location**: `app/uam/src/test/java/com/anasdidi/uam/controller/impl/UserControllerV1Tests.java:145-161`
- **Why it matters**: `E02_RESOURCE_ALREADY_EXISTS.httpStatus` was changed from `HttpStatus.BAD_REQUEST` (400) to `HttpStatus.CONFLICT` (409). The mock returns a DTO with this enum, and the controller calls `ResponseEntity.status(res.getResponse().httpStatus)`. The test asserts `status().isBadRequest()` which will return 200-series "expected 400 but got 409" failure.
- **Evidence**:
  - `ResponseEnum.java:11` — `E02_RESOURCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "E02", ...)`
  - `UserControllerV1.java:40` — `return ResponseEntity.status(res.getResponse().httpStatus).body(res);`
  - `UserControllerV1Tests.java:158` — `.andExpect(status().isBadRequest())` ← will get 409
- **Fix**: Change `.andExpect(status().isBadRequest())` to `.andExpect(status().isConflict())`.

### [P1] High — Controller test asserts `isBadRequest()` for `E03_RESOURCE_NOT_FOUND` but HTTP status changed to `NOT_FOUND`

- **Location**: `app/uam/src/test/java/com/anasdidi/uam/controller/impl/UserControllerV1Tests.java:244-259`
- **Why it matters**: Same pattern. `E03_RESOURCE_NOT_FOUND.httpStatus` was changed from `BAD_REQUEST` to `NOT_FOUND` (404). The mock also changes via the enum. Test expects 400 but will get 404.
- **Evidence**:
  - `ResponseEnum.java:12` — `E03_RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "E03", ...)`
  - `UserControllerV1Tests.java:257` — `.andExpect(status().isBadRequest())` ← will get 404
- **Fix**: Change `.andExpect(status().isBadRequest())` to `.andExpect(status().isNotFound())`.

### [P2] Medium — Controller tests use `S00_SUCCESS` and `isOk()` for register success mock, semantically inconsistent with real behavior

- **Location**: `app/uam/src/test/java/com/anasdidi/uam/controller/impl/UserControllerV1Tests.java:60, 187`
- **Why it matters**: The mock-based controller tests for register success build the response with `ResponseEnum.S00_SUCCESS` and assert `status().isOk()`. These tests won't fail (mocks bypass the real service), but they no longer reflect the actual behavior (register now returns 201 Created). This creates a gap between test scenarios and production behavior.
- **Evidence**:
  - Line 60: `.response(ResponseEnum.S00_SUCCESS)` in mock response builder
  - Line 71: `.andExpect(status().isOk())`
  - Real service now returns `S01_CREATED` → HTTP 201
- **Fix**: Update the mock to use `ResponseEnum.S01_CREATED` and assert `status().isCreated()` for register success tests. Not a blocking issue since mocks are controlled, but contributes to test drift.

## Suggested Next Steps

- [ ] Fix `RegisterUserServiceTests.java` assertions in both test methods (lines 46, 50, 51 and 307-309)
- [ ] Fix `UserControllerV1Tests.java` test named `testRegisterUser_serviceReturnsE02_returnsBadRequest` — change to `isConflict()`
- [ ] Fix `UserControllerV1Tests.java` test named `testGetUser_serviceReturnsE03_returnsBadRequest` — change to `isNotFound()`
- [ ] (Optional) Update `testRegisterUser_success` and `testRegisterUser_jsonResponseStructure` in `UserControllerV1Tests.java` to use `S01_CREATED` and `isCreated()` for semantic correctness
- [ ] Run `./mvnw test -pl uam -am` to confirm all tests pass after fixes
- [ ] Run `./mvnw spotless:apply && ./mvnw verify` for full validation
