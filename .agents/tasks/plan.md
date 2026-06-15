# Plan: Controller & Service Test Implementation

## Objective

Refactor and extend test coverage for the `uam` module's controller and service layers. Rewrite controller tests from pure Mockito (`MockMvcBuilders.standaloneSetup`) to `@WebMvcTest` framework. Keep service tests as `@SpringBootTest` integration tests. Achieve > 80% line coverage across both layers.

## Requirements Snapshot

- **R1:** Controller tests must use `@WebMvcTest` (not `MockitoExtension` + `standaloneSetup`).
- **R2:** Service tests must remain integration tests (`@SpringBootTest`).
- **R3:** Overall line coverage > 80% across `uam` module's controller and service code.
- **R4:** Existing test assertions must be preserved where they remain valid.

## Scope

- Rewrite `uam/src/test/.../controller/impl/HelloWorldControllerV1Tests.java` to use `@WebMvcTest`.
- Extend `uam/src/test/.../service/impl/HelloWorldServiceTests.java` with additional edge-case tests.
- Add `JaCoCo` Maven plugin to the `uam` module for coverage measurement.
- Add test-specific `application.yml` under `uam/src/test/resources/` if needed for service tests.
- Run full test suite and verify coverage exceeds 80%.

## Out of Scope

- Tests for the `common` module (no `src/test/` exists yet).
- Tests for the aspect layer (`ExecuteTraceAspect`) directly — it is exercised indirectly via service integration tests.
- Liquibase, JPA, or repository tests (no entities/repos exist yet).
- Security testing (security starter is commented out).

## Assumptions and Constraints

- The Spring Boot 4.0.6 / JUnit 5 / Mockito stack bundled with `spring-boot-starter-test` is available.
- `spring-boot-starter-test` includes `@WebMvcTest` support via `spring-boot-test-autoconfigure`.
- `@WebMvcTest(HelloWorldControllerV1.class)` does **not** load `@Component`/`@Service` beans — only the controller under test. We use `@MockBean` for `HelloWorldService`.
- `@SpringBootTest` in service tests loads the full context, including `ExecuteTraceAspect`. This means `ConstraintViolationException` thrown inside the `@Valid` method is **caught by the aspect** and translated into an E01 response — it does NOT propagate to the caller. (Currently the service tests assert `assertThrows(ConstraintViolationException.class, ...)` which would **fail** if the aspect is loaded, so those tests need to be updated.)

## Risks and Areas Requiring Care

1. **Aspect vs. `@Validated` interaction (HIGH):** `HelloWorldService.execute(@Valid ...)` triggers `ConstraintViolationException` inside `joinPoint.proceed()`. The `ExecuteTraceAspect` catches it and returns a response with `E01_VALIDATION_ERROR` **without re-throwing**. Existing service tests `assertThrows(ConstraintViolationException.class, ...)` will **fail** when the aspect is active. Must rewrite them to expect a normal return with E01 fields instead.
2. **Coverage gap for `@WebMvcTest` error paths:** With a mocked service, `ConstraintViolationException` from validation won't occur naturally (Spring MVC request validation at the controller parameter level would produce 400 errors before the service is called). Need to explicitly decide which paths are testable in each layer.
3. **No JaCoCo configured yet:** Must add and configure it for `uam` module only.

## Core Concepts

### `@WebMvcTest` vs `MockMvcBuilders.standaloneSetup`

| Concern | `MockitoExtension` + `standaloneSetup` (current) | `@WebMvcTest` (target) |
|---|---|---|
| Context loading | None (unit test) | Slices web layer only |
| MockMvc | Manually built | `@Autowired` |
| Service mock | `@Mock` | `@MockBean` |
| Exception handling | Manual setup | Full Spring MVC machinery |
| JSON serialization | Manual if needed | Auto-configured `ObjectMapper` |

Before (current):
```java
@ExtendWith(MockitoExtension.class)
class HelloWorldControllerV1Tests {
    @Mock HelloWorldService helloWorldService;
    @InjectMocks HelloWorldControllerV1 helloWorldControllerV1;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(helloWorldControllerV1).build();
    }
}
```

After (target):
```java
@WebMvcTest(HelloWorldControllerV1.class)
class HelloWorldControllerV1Tests {
    @Autowired private MockMvc mockMvc;
    @MockBean private HelloWorldService helloWorldService;
    // No setUp() needed
}
```

### Aspect + `@Validated` flow (service integration test)

```
Caller → Service Proxy → @Validated interceptor → ExecuteTraceAspect → joinPoint.proceed()
                                                                         │
                                                      ConstraintViolationException
                                                                         │
                                                      └─ Aspect catches it
                                                         Sets E01 on response
                                                         Returns response (no re-throw)
```

This means: in `@SpringBootTest`, calling `helloWorldService.execute(invalidReq)` **returns a response** with error fields — it does **not** throw `ConstraintViolationException`.

---

## Sub-Tasks

### Sub-Task 1: Rewrite Controller Tests to Use `@WebMvcTest`

- **Status:** Pending
- **Objective:** Replace the current `@ExtendWith(MockitoExtension.class)` + `MockMvcBuilders.standaloneSetup()` pattern with `@WebMvcTest(HelloWorldControllerV1.class)` and extend coverage.
- **Related Requirements:** R1, R3
- **Dependencies and Preconditions:** None
- **In Scope for This Sub-Task:**
  - Change test class annotation from `@ExtendWith(MockitoExtension.class)` to `@WebMvcTest(HelloWorldControllerV1.class)`.
  - Replace `@Mock` + `@InjectMocks` with `@MockBean`.
  - Replace `MockMvcBuilders.standaloneSetup()` with `@Autowired MockMvc mockMvc`.
  - Remove `@BeforeEach setUp()`.
  - Preserve existing happy-path and missing-name tests.
  - Add the following new test cases for > 80% coverage:
    - **Missing correlationId header** → 400 (Spring MVC requires the header per the controller interface)
    - **Null/empty name param** → 400 (Spring MVC treats `required=true` + empty string differently from absent param; test both)
    - **Service returns E01 error response** → mock returns response with `ResponseEnum.E01_VALIDATION_ERROR` → verify HTTP 400 + E01 code and desc in JSON
    - **Service returns E99 error response** → mock returns response with `ResponseEnum.E99_UNEXPECTED_ERROR` → verify HTTP 500 + E99 code and desc in JSON
    - **JSON response structure** — verify `correlationId`, `responseCode`, `responseDesc`, `payload.greeting` are present on success
- **Out of Scope for This Sub-Task:**
  - Aspect-level behavior (tested in service integration tests)
  - Exception-throwing service mock (the controller never sees exceptions from the service — the aspect handles them in production; in `@WebMvcTest`, the aspect is not loaded, so any exception from the mock would produce a generic 500, which is not a meaningful coverage target)
- **Instructions:**
  - Replace the file at `app/uam/src/test/java/com/anasdidi/uam/controller/impl/HelloWorldControllerV1Tests.java`.
  - Use `@WebMvcTest(HelloWorldControllerV1.class)` — this pulls in Spring MVC auto-configuration.
  - `@Autowired MockMvc mockMvc`.
  - `@MockBean HelloWorldService helloWorldService`.
  - For each test that returns error codes via the mock, construct the `HelloWorldResDTO` with the appropriate `ResponseEnum` and verify the HTTP status and error fields.
  - Use `jsonPath("$.responseCode")`, `jsonPath("$.responseDesc")` etc.
- **Acceptance Criteria:**
  - All controller tests pass.
  - No `MockMvcBuilders.standaloneSetup` usage remains.
  - At least 6 test methods covering the scenarios listed above.
- **Cautionary Points (Risks & Edge Cases):**
  - `@WebMvcTest` does NOT load `@Service`, `@Component`, or `@Aspect` beans — the `ExecuteTraceAspect` is absent. Error handling via the aspect does not apply; the mock returns whatever we tell it.
  - Missing `name` query param: `@RequestParam(required = true)` makes Spring return 400 automatically — no service call happens.
  - Missing `App-Correlation-Id` header: the interface declares `@RequestHeader(name = CommonConstants.HEADER_CORR_ID)` — Spring returns 400 with missing header error. Test can just verify HTTP 400.
  - Ensure the test class is package-private (`class` not `public class`) to match existing style.
- **Testing Suggestions:**
  - Run: `./mvnw test -pl uam -am -Dtest=HelloWorldControllerV1Tests`
  - All 6+ tests should pass.
- **Done When:** All declared controller tests pass on `./mvnw test -pl uam -am -Dtest=HelloWorldControllerV1Tests`, and no `StandaloneMockMvcBuilder` usage exists in the file.

---

### Sub-Task 2: Update and Extend Service Integration Tests

- **Status:** Pending
- **Objective:** Update existing service tests for correctness with the active `ExecuteTraceAspect`, and add additional edge cases to push coverage > 80%.
- **Related Requirements:** R2, R3
- **Dependencies and Preconditions:** None
- **In Scope for This Sub-Task:**
  - **Rewrite `testGreeting_missingCorrelationId_throwsConstraintViolationException`** to expect a normal return with error fields instead of `assertThrows`. The aspect catches `ConstraintViolationException` and returns a `HelloWorldResDTO` with `response = E01_VALIDATION_ERROR`. Verify:
    - No exception thrown.
    - `responseCode` = "E01", `responseDesc` = "Validation Error".
    - `correlationId` is set (even though blank, the aspect reads it before validation).
    - `traceId`, `timestamp`, `timeTaken` are populated by the aspect.
  - **Same rewrite for `testGreeting_missingName_throwsConstraintViolationException`** — expect E01 error response, not an exception.
  - Add new test cases:
    - **Empty string name (`name=""`) with valid correlationId** → E01 validation error (aspect catches `ConstraintViolationException` for the `@NotBlank name`).
    - **Name with special characters** → verify greeting is `"Hi, <exact input>"` (no sanitization).
    - **Name with leading/trailing whitespace** → verify greeting preserves whitespace (pass-through).
    - **Very long name** → verify greeting works (no truncation).
    - **Null payload** — if possible (the payload is `@Valid @NonNull`, so null payload would trigger validation before method call — E01 response).
- **Out of Scope for This Sub-Task:**
  - Testing `ServiceError` / `E99UnexpectedError` paths (the service never throws these; aspect tests for common module are out of scope).
  - Testing the aspect directly.
- **Instructions:**
  - Edit `app/uam/src/test/java/com/anasdidi/uam/service/impl/HelloWorldServiceTests.java`.
  - Change the two `assertThrows(ConstraintViolationException.class, ...)` to call `execute()` normally and assert E01 response fields.
  - Add new test methods following the same pattern: build request, call `execute()`, assert fields on the returned DTO.
  - For happy-path tests, continue asserting `traceId`, `timestamp`, and `timeTaken` are non-null.
  - For error-path tests, assert `responseCode` = "E01" and `responseDesc` = "Validation Error".
- **Acceptance Criteria:**
  - All service tests pass.
  - No `assertThrows(ConstraintViolationException.class, ...)` remains.
  - At least 7 test methods covering happy path, correlationId blank, name blank, name empty, special chars, whitespace, long name, and null payload.
- **Cautionary Points (Risks & Edge Cases):**
  - **This is the highest risk sub-task.** The `ExecuteTraceAspect` catches `ConstraintViolationException` and returns a response. Existing tests that assert exception propagation will **FAIL** once the aspect is properly loaded. The rewriting is mandatory.
  - If the `@SpringBootTest` does NOT load the aspect (edge case), the tests would continue to pass with the old `assertThrows` pattern. Verify by checking the response in the new tests — if `responseCode` is "E01" instead of an exception being thrown, the aspect is active.
  - `@Data` on DTOs means `setResponse()` is available. The aspect calls `res.setResponse(ResponseEnum.E01_VALIDATION_ERROR)` on the **pre-created empty response** it made with `objectMapper.convertValue(...)`. However, in the service impl, we return a **new** response object (created via `HelloWorldResDTO.builder()...build()`). The aspect's `res = (BaseResDTO) joinPoint.proceed()` replaces the empty response with the returned one, so the returned response object has `response` set by the service impl to `S00_SUCCESS`. On validation error, the aspect catches the exception and calls `setResponse(E01)` on its original empty `res` object. So the E01 is set on the aspect's response, not on the service's. This should still work correctly.
  - Verify that `correlationId` on the error response is set. The aspect creates `res` using `objectMapper.convertValue(Map.of("correlationId", req.getCorrelationId()), returnClass)`, so even with blank correlationId, the value is set.
- **Testing Suggestions:**
  - Run: `./mvnw test -pl uam -am -Dtest=HelloWorldServiceTests`
  - All 7+ tests should pass.
  - To verify the aspect is loaded, temporarily add a `log.info()` or debugger in the test — but the E01 response code assertion is the strongest signal.
- **Done When:** All service tests pass, all `assertThrows(ConstraintViolationException.class)` are replaced, and at least 7 test methods exist.

---

### Sub-Task 3: Add JaCoCo Maven Plugin for Coverage Verification

- **Status:** Pending
- **Objective:** Add the JaCoCo Maven plugin to the `uam` module to measure and verify > 80% line coverage.
- **Related Requirements:** R3
- **Dependencies and Preconditions:** Sub-Task 1 and Sub-Task 2 must be completed (tests must pass first).
- **In Scope for This Sub-Task:**
  - Add `org.jacoco:jacoco-maven-plugin` to `app/uam/pom.xml`.
  - Configure `prepare-agent` goal (pre-integration-test phase).
  - Configure `report` goal (post-integration-test phase) to generate HTML/XML reports.
  - Optionally configure a `check` goal with `counter="LINE"`, `value="COVEREDRATIO"`, `minimum="0.80"` to enforce 80% line coverage for the `com.anasdidi.uam.controller.impl` and `com.anasdidi.uam.service.impl` packages.
  - Run `./mvnw clean verify -pl uam -am` and verify JaCoCo report is generated.
- **Out of Scope for This Sub-Task:**
  - JaCoCo for the `common` module (no tests exist there yet).
  - Branch coverage — line coverage is sufficient for the > 80% requirement.
  - CI/CD integration.
- **Instructions:**
  - Add the plugin under `<build><plugins>` in `app/uam/pom.xml`:
    ```xml
    <plugin>
      <groupId>org.jacoco</groupId>
      <artifactId>jacoco-maven-plugin</artifactId>
      <version>0.8.12</version>
      <executions>
        <execution>
          <id>prepare-agent</id>
          <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
          <id>report</id>
          <phase>verify</phase>
          <goals><goal>report</goal></goals>
        </execution>
        <execution>
          <id>check</id>
          <phase>verify</phase>
          <goals><goal>check</goal></goals>
          <configuration>
            <rules>
              <rule>
                <element>PACKAGE</element>
                <includes>
                  <include>com.anasdidi.uam.controller.impl</include>
                  <include>com.anasdidi.uam.service.impl</include>
                </includes>
                <limits>
                  <limit>
                    <counter>LINE</counter>
                    <value>COVEREDRATIO</value>
                    <minimum>0.80</minimum>
                  </limit>
                </limits>
              </rule>
            </rules>
          </configuration>
        </execution>
      </executions>
    </plugin>
    ```
- **Acceptance Criteria:**
  - `./mvnw clean verify -pl uam -am` succeeds.
  - JaCoCo report is generated at `app/uam/target/site/jacoco/index.html`.
  - Coverage check passes (≥ 80% line coverage on the specified packages).
- **Cautionary Points (Risks & Edge Cases):**
  - JaCoCo `check` can fail the build if coverage is below the threshold. Start with `minimum="0.80"` but if tests don't quite reach it, adjust coverage gap items in Sub-Tasks 1/2 first, then raise the minimum.
  - The `check` execution can be made to fail the build (`haltOnFailure=true` by default; for JaCoCo 0.8.12 this is the behavior).
  - JaCoCo 0.8.12 works with Java 25 (latest JaCoCo as of 2026 supports Java 25).
  - Ensure no conflicting bytecode instrumentation with Spring AOP — this is a standard combination.
- **Testing Suggestions:**
  - `./mvnw clean verify -pl uam -am` — full pipeline including Spotless and JaCoCo.
  - Open `app/uam/target/site/jacoco/index.html` in a browser to inspect coverage.
  - If the check fails, inspect which lines/methods are uncovered and add missing tests in Sub-Tasks 1/2.
- **Done When:** `./mvnw clean verify -pl uam -am` succeeds and the JaCoCo report shows ≥ 80% line coverage for `com.anasdidi.uam.controller.impl` and `com.anasdidi.uam.service.impl`.

---

### Sub-Task 4: Final Verification & Coverage Gap Closure

- **Status:** Pending
- **Objective:** Run the full pipeline, inspect the JaCoCo report, and close any coverage gaps below 80%.
- **Related Requirements:** R3
- **Dependencies and Preconditions:** Sub-Tasks 1, 2, and 3 completed.
- **In Scope for This Sub-Task:**
  - Run `./mvnw clean verify -pl uam -am`.
  - If the JaCoCo `check` fails, inspect the report to identify uncovered lines.
  - Add or adjust test cases in the controller or service test classes to cover the missing lines.
  - Repeat until `check` passes with ≥ 80%.
  - Ensure all tests pass and no formatting issues exist (`spotless:check` runs during `verify`).
- **Out of Scope for This Sub-Task:**
  - Adding new production code.
  - Changing existing production logic.
- **Instructions:**
  - Run the full pipeline first to see if it passes.
  - If coverage is below 80%, inspect the report. Likely uncovered areas:
    - The `HelloWorldControllerV1.greeting()` method is fully covered by the 6+ test cases.
    - The `HelloWorldService.execute()` method is covered by happy-path + multiple validation-error tests.
    - The private `HelloWorldService.prepareGreeting()` method is indirectly covered by happy-path tests.
  - If coverage is still low, consider:
    - Testing additional DTO field accessors indirectly.
    - Adding edge-case controller tests for different content types or malformed requests.
    - Adding a service test with a null payload (if not already covered).
- **Acceptance Criteria:**
  - `./mvnw clean verify -pl uam -am` passes.
  - JaCoCo report shows ≥ 80% line coverage for both target packages.
- **Cautionary Points (Risks & Edge Cases):**
  - Spotless formatting check runs during `verify`. If tests pass but formatting fails, run `./mvnw spotless:apply -pl uam -am` first.
  - If the coverage is very close (e.g., 78%), adding one or two test cases should close the gap.
- **Testing Suggestions:**
  - `./mvnw clean verify -pl uam -am 2>&1 | tee verification.log`
- **Done When:** The full `verify` pipeline completes successfully with all tests passing, Spotless passing, and JaCoCo coverage ≥ 80%.

---

## Final Integration & Verification

- **System-Wide Test:** `./mvnw clean verify -pl uam -am`
  - Compiles `common` and `uam`.
  - Runs Spotless `check`.
  - Runs all tests.
  - Generates JaCoCo report.
- **Completion Checklist:**
  - [ ] Controller tests use `@WebMvcTest` (Sub-Task 1).
  - [ ] All controller test scenarios pass (≥ 6 tests).
  - [ ] Service tests use `@SpringBootTest` (Sub-Task 2).
  - [ ] `assertThrows(ConstraintViolationException.class)` replaced with E01 response assertions (Sub-Task 2).
  - [ ] All service test scenarios pass (≥ 7 tests).
  - [ ] JaCoCo plugin configured in `uam/pom.xml` (Sub-Task 3).
  - [ ] Coverage ≥ 80% for `com.anasdidi.uam.controller.impl` and `com.anasdidi.uam.service.impl` (Sub-Task 4).
  - [ ] `./mvnw clean verify -pl uam -am` passes end-to-end.
