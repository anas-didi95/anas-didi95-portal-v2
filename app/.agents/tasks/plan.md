# Plan: Implement `@ExecuteTrace` AOP Logging Aspect

## Objective

Implement an AOP annotation (`@ExecuteTrace`) and its corresponding aspect (`ExecuteTraceAspect`) in the `common` module so that every subclass of `IBaseService` automatically logs incoming request parameters and outgoing responses (or errors) when its `execute` method is called.

## Requirements Snapshot

- **R1:** Define `@ExecuteTrace` as a runtime-retained, method-level annotation.
- **R2:** Create `ExecuteTraceAspect` as a Spring `@Aspect @Component` that intercepts `@ExecuteTrace`-annotated methods.
- **R3:** Log incoming request parameter **before** execution and outgoing response (or error) **after** execution.
- **R4:** The aspect must work with reactive types (`Mono<B>`) — log on `Mono` completion/error, not just at method-entry time.
- **R5:** No changes to `IBaseService.java` — it already has `@ExecuteTrace` on the `execute` method.
- **R6:** Add required AOP dependency to `common/pom.xml`.
- **R7:** The change must compile and all existing tests must pass after the change.

## Scope

- Update `ExecuteTrace.java` with proper retention/target meta-annotations.
- Rewrite `ExecuteTraceAspect.java` with correct class name and full aspect logic.
- Add `spring-boot-starter-aop` to `common/pom.xml`.
- Verify compilation and full test suite passes.
- **Out of scope:** Adding new tests for the aspect itself (but validation against existing tests is required).

## Assumptions and Constraints

- The `common` module is a library JAR; the aspect will be picked up by Spring component scanning in `uam` because `@SpringBootApplication` scans `com.anasdidi` packages (including `com.anasdidi.common`).
- `IBaseService` already has `@ExecuteTrace` on the `execute` method — no change needed there.
- No `@EnableAspectJAutoProxy` is needed explicitly — `spring-boot-starter-aop` auto-configures it in Spring Boot.
- The project uses GOOGLE style formatting via Spotless — run `spotless:apply` after all edits.
- SLF4J + Lombok `@Slf4j` is the logging approach (Lombok is already a dependency in `common`).

## Risks and Areas Requiring Care

- The `ExecuteTraceAspect.java` file currently has class name `ExecuteTraceLog` (a stub) — the file must be **rewritten entirely**, not patched.
- The AOP must work with reactive `Mono` returns — naive `@Around` that logs after `proceed()` would log the `Mono` object, not the actual response. The aspect must wrap/transform the `Mono`.
- The `HelloWorldServiceTests` are `@SpringBootTest` and will load the full context — the aspect will be active during tests. The tests must still pass (the aspect should not break existing behavior).
- The `HelloWorldControllerV1Tests` use `@WebFluxTest` with a `@TestConfiguration` that mocks the service — the aspect will NOT be active there (no `@SpringBootTest`), but that's fine.

## Core Concepts

### Reactive AOP with Mono

When the target method returns `Mono<B>`, a naive `@Around` aspect that does:

```java
Object result = pjp.proceed();
log.info("Response: {}", result); // Logs Mono object, not the actual value!
return result;
```

would log the `Mono` wrapper, not the resolved value. Instead, the aspect must transform the `Mono`:

```java
@Around("@annotation(com.anasdidi.common.aspect.ExecuteTrace)")
public Object trace(ProceedingJoinPoint pjp) throws Throwable {
    // Log request
    Object[] args = pjp.getArgs();
    log.info("Request: {}", args);

    Mono<?> result = (Mono<?>) pjp.proceed();
    return result
        .doOnSuccess(res -> log.info("Response: {}", res))
        .doOnError(err -> log.error("Error: {}", err.getMessage()));
}
```

This logs the request immediately (before the method body executes), and logs the response or error when the `Mono` actually completes.

## Sub-Tasks

### Sub-Task 1: Add `spring-boot-starter-aop` dependency to `common/pom.xml`

- **Status:** Completed
- **Objective:** Add the Spring Boot AOP starter so `@Aspect`, `@Around`, `ProceedingJoinPoint`, etc. are available at compile time and AOP auto-configuration activates at runtime.
- **Related Requirements:** R2, R6
- **Dependencies and Preconditions:** None.
- **In Scope:**
  - Add `<groupId>org.springframework.boot</groupId>` / `<artifactId>spring-boot-starter-aop</artifactId>` to the `<dependencies>` section of `common/pom.xml`.
  - This is **not** marked `optional` — consumers (uam) need it transitively.
- **Out of Scope:** No changes to `uam/pom.xml`.
- **Instructions:**
  1. Read `common/pom.xml`.
  2. Insert the `spring-boot-starter-aop` dependency after the existing `spring-boot-starter-webflux` entry.
- **Acceptance Criteria:** `./mvnw compile -pl common` succeeds.
- **Cautionary Points:** The Spring Boot parent POM (`4.0.6`) manages the version — no `<version>` tag needed.
- **Implementation Suggestions:** Place it right after the `spring-boot-starter-webflux` entry in the dependencies list.
- **Testing Suggestions:**
  ```bash
  ./mvnw compile -pl common
  ```
- **Done When:** `common/pom.xml` has the `spring-boot-starter-aop` dependency and compiles cleanly.

### Sub-Task 2: Update `ExecuteTrace.java` with proper annotation meta-annotations

- **Status:** Completed
- **Objective:** Make `@ExecuteTrace` a runtime-retained, method-level annotation so the aspect can intercept it.
- **Related Requirements:** R1
- **Dependencies and Preconditions:** None.
- **In Scope:**
  - Add `@Retention(RetentionPolicy.RUNTIME)` — required by Spring AOP to detect the annotation at runtime.
  - Add `@Target(ElementType.METHOD)` — restricts to method-level usage.
  - Keep the `package` and `public @interface ExecuteTrace` declaration.
- **Out of Scope:** No other meta-annotations (e.g., `@Documented`, `@Inherited`) unless they add value.
- **Instructions:**
  1. Read the current `ExecuteTrace.java`.
  2. Add the two meta-annotation imports and annotations.
- **Acceptance Criteria:** The annotation compiles and can be intercepted by `@Around("@annotation(ExecuteTrace)")`.
- **Implementation Suggestions:**
  ```java
  package com.anasdidi.common.aspect;
  
  import java.lang.annotation.ElementType;
  import java.lang.annotation.Retention;
  import java.lang.annotation.RetentionPolicy;
  import java.lang.annotation.Target;
  
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface ExecuteTrace {}
  ```
- **Testing Suggestions:** `./mvnw compile -pl common` succeeds.
- **Done When:** `ExecuteTrace.java` has `@Retention(RUNTIME)` and `@Target(METHOD)` and compiles.

### Sub-Task 3: Rewrite `ExecuteTraceAspect.java` with full aspect logic

- **Status:** Completed
- **Objective:** Create a Spring `@Aspect @Component` that intercepts `@ExecuteTrace`, logs the incoming request arguments, proceeds with the call, and logs the response/error from the reactive `Mono`.
- **Related Requirements:** R2, R3, R4
- **Dependencies and Preconditions:** Sub-Task 1 and Sub-Task 2 must be done (AOP dependency + annotation fixed).
- **In Scope:**
  - Change class name from `ExecuteTraceLog` to `ExecuteTraceAspect`.
  - Add `@Aspect` and `@Component` class-level annotations.
  - Add `@Slf4j` from Lombok for logging.
  - Implement `@Around("@annotation(com.anasdidi.common.aspect.ExecuteTrace)")` advice.
  - Log request parameters with `log.info("Request: {}", args)`.
  - Cast the `proceed()` result to `Mono<?>`.
  - Use `.doOnSuccess()` to log the response.
  - Use `.doOnError()` to log errors.
  - Return the transformed `Mono`.
- **Out of Scope:**
  - Do NOT add method-level validation or exception handling — that's the service's job.
  - Do NOT log sensitive fields (the DTO's own `toString()` controls what is exposed).
- **Instructions:**
  1. Read the current `ExecuteTraceAspect.java`.
  2. Replace the entire content with a proper aspect implementation.
- **Acceptance Criteria:**
  - Compiles with `./mvnw compile -pl common`.
  - Full build succeeds: `./mvnw verify -pl uam -am`.
  - Existing tests pass.
- **Implementation Suggestions:**
  ```java
  package com.anasdidi.common.aspect;
  
  import lombok.extern.slf4j.Slf4j;
  import org.aspectj.lang.ProceedingJoinPoint;
  import org.aspectj.lang.annotation.Around;
  import org.aspectj.lang.annotation.Aspect;
  import org.springframework.stereotype.Component;
  import reactor.core.publisher.Mono;
  
  @Slf4j
  @Aspect
  @Component
  public class ExecuteTraceAspect {
  
    @Around("@annotation(com.anasdidi.common.aspect.ExecuteTrace)")
    public Object traceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
      Object[] args = joinPoint.getArgs();
      log.info("Request: {}", args);
  
      Mono<?> result = (Mono<?>) joinPoint.proceed();
  
      return result
          .doOnSuccess(res -> log.info("Response: {}", res))
          .doOnError(err -> log.error("Execution error", err));
    }
  }
  ```
- **Testing Suggestions:**
  ```bash
  ./mvnw compile -pl common                    # Compile common
  ./mvnw test -pl uam -am                      # Run all uam tests (aspect should be active for service tests)
  ```
- **Done When:** The aspect rewrites `ExecuteTraceAspect.java`, compiles, and all existing tests in `uam` pass.

### Sub-Task 4: Apply Spotless formatting and verify full pipeline

- **Status:** Completed
- **Objective:** Ensure all changed files conform to the project's formatting standards, and the full build pipeline passes.
- **Related Requirements:** R7
- **Dependencies and Preconditions:** Sub-Tasks 1, 2, and 3 complete.
- **In Scope:**
  - Run `./mvnw spotless:apply` to auto-format all changed files.
  - Run `./mvnw verify` to run Spotless check + compilation + tests.
- **Out of Scope:** No further source code changes.
- **Instructions:**
  1. Run `./mvnw spotless:apply`.
  2. Run `./mvnw verify`.
- **Acceptance Criteria:** `./mvnw verify` exits with `BUILD SUCCESS`.
- **Cautionary Points:** Spotless `check` runs during `verify` phase — must `apply` first or `verify` will fail if formatting is off.
- **Testing Suggestions:**
  ```bash
  ./mvnw spotless:apply && ./mvnw verify
  ```
- **Done When:** Full `verify` passes with no formatting violations and all tests green.

## Final Integration & Verification

- **System-Wide Test:** After all sub-tasks, run:
  ```bash
  ./mvnw clean verify
  ```
  This compiles both modules, runs Spotless check, and runs all tests.

- **Completion Checklist:**
  - [x] `common/pom.xml` has `spring-boot-starter-aspectj` added (Spring Boot 4.x name).
  - [x] `ExecuteTrace.java` has `@Retention(RUNTIME)` and `@Target(METHOD)` (pre-existing).
  - [x] `ExecuteTraceAspect.java` rewritten with `@Aspect`, `@Component`, `@Around`, reactive-aware logging, `Arrays.toString()`, `instanceof` guard, method signature in logs.
  - [x] `UamApplication.java` updated with `scanBasePackages = "com.anasdidi"` (fix P0 component scan gap).
  - [x] `IBaseService.java` unchanged (already has `@ExecuteTrace`).
  - [x] `./mvnw clean verify` passes (6/6 tests, Spotless clean).
  - [x] No manual formatting — cleaned by `spotless:apply`.

## Open Questions

- None.
