# Plan: Refactor UAM to Multi-Module with Common Library

## Objective

Split the existing single-module Maven project (`app/uam`) into a multi-module build where a new `common` module (library) is built first, and `uam` depends on it as a Maven dependency. The `com.anasdidi.uam.common` package (constants, marker interfaces, enums) moves into the `common` module. Dependency management is centralized in the parent POM.

## Requirements Snapshot

- **R1:** Multi-module Maven build — `common` is built first, then `uam` depends on `common`.
- **R2:** Define dependency management in the parent POM for common dependencies.
- **R3:** Move the `com.anasdidi.uam.common` package into a new `common` project.

### Package to move (`com.anasdidi.uam.common`)
| File | Dependencies |
|---|---|
| `CommonConstants.java` | `lombok.experimental.UtilityClass` |
| `IBaseReqDTO.java` | none |
| `IBaseResDTO.java` | none |
| `enums/ResponseEnum.java` | `lombok.RequiredArgsConstructor`, `org.springframework.http.HttpStatus` |

### Files in `uam` that import from the common package
| File | Imports |
|---|---|
| `controller/HelloWorldController.java` | `CommonConstants` (HEADER_CORR_ID) |
| `controller/impl/HelloWorldControllerV1.java` | `CommonConstants` (CONTEXT_PATH, API_V1) |
| `dto/HelloWorldReqDTO.java` | `IBaseReqDTO` |
| `dto/HelloWorldResDTO.java` | `IBaseResDTO`, `ResponseEnum` |
| `service/UamService.java` | `IBaseReqDTO`, `IBaseResDTO` |
| `service/impl/HelloWorldService.java` | `ResponseEnum` |
| `test/.../HelloWorldServiceTests.java` | `ResponseEnum` |
| `test/.../HelloWorldControllerV1Tests.java` | `ResponseEnum` |

## Scope

- **In scope:**
  - Create parent POM at `app/pom.xml` (pom packaging, manages modules, dependency management).
  - Create `app/common/` module with its own POM, containing the moved common package.
  - Move `src/main/java/com/anasdidi/uam/common/**` from `uam` to `common`.
  - Add `com.anasdidi:uam-common` as a dependency in `uam/pom.xml`.
  - Update `uam/pom.xml` parent reference to point to the new parent POM.
  - Keep all Java imports unchanged (package path `com.anasdidi.uam.common` remains the same — only the artifact changes).
  - Keep `spring-boot-starter-parent` as parent of the root `app/pom.xml` so all modules inherit managed dependency versions.
  - Ensure `spring-boot-maven-plugin` is only applied to the `uam` module.
  - Build + test verification that everything compiles and tests pass.

- **Out of scope:**
  - Package rename (no change from `com.anasdidi.uam.common`).
  - Modifying any business logic.
  - Adding new dependencies beyond what the common module needs.
  - CI/CD pipeline changes.
  - Docker or dev container changes.

## Assumptions and Constraints

- **Spring Boot 4.0.6** parent is used — `app/pom.xml` will be the direct child of `spring-boot-starter-parent`.
- **Java 25**, Maven wrapper (`./mvnw`), Spotless formatting defined in parent POM and applied to all modules.
- `common` is a plain library JAR (not a Spring Boot application) — no `spring-boot-maven-plugin`.
- The build order (`common` → `uam`) is guaranteed by Maven's reactor when modules are declared in the parent POM in that order.
- The `uam-common` artifact version tracks `app`'s version (`0.0.1-SNAPSHOT`).

## Risks and Areas Requiring Care

- **Parent POM conflict:** `uam/pom.xml` currently has `spring-boot-starter-parent` as its parent. After refactor, `spring-boot-starter-parent` moves to `app/pom.xml`. The `uam/pom.xml` must correctly inherit from `app/pom.xml` via `<relativePath>../pom.xml</relativePath>`.
- **spring-boot-maven-plugin:** Must not be inherited by `common`. The parent POM should define it in `<pluginManagement>` only, and `uam/pom.xml` explicitly declares it in `<build><plugins>`.
- **Annotation processor config for Lombok:** The `maven-compiler-plugin` config in `uam/pom.xml` must be preserved.
- **Spotless plugin:** Must move from `uam/pom.xml` into the parent POM (`app/pom.xml`). Placed in `<build><plugins>` (not just `<pluginManagement>`) so all modules inherit it automatically. Remove it from `uam/pom.xml` to avoid duplication.
- **Package naming:** Keep `com.anasdidi.uam.common` in the common module — this means the package doesn't match the artifactId (`uam-common`), but that is by design for minimal import disruption.

## Core concepts

### Multi-module Maven structure (before vs after)

**Before:**
```
app/uam/
  pom.xml           ← parent = spring-boot-starter-parent
  src/main/java/com/anasdidi/uam/common/...
```

**After:**
```
app/
  pom.xml           ← parent = spring-boot-starter-parent, packaging = pom, modules = [common, uam]
  common/
    pom.xml         ← parent = app/pom.xml, artifactId = uam-common
    src/main/java/com/anasdidi/uam/common/{CommonConstants,IBaseReqDTO,IBaseResDTO,enums/ResponseEnum}.java
  uam/
    pom.xml         ← parent = app/pom.xml, depends on com.anasdidi:uam-common
    src/...         ← same as before except common/ package removed
```

### How imports stay unchanged

The Java package in the `common` module remains `com.anasdidi.uam.common`. When `uam` depends on `uam-common`, the classpath includes both `uam`'s classes and `common`'s classes. Imports like `import com.anasdidi.uam.common.CommonConstants` resolve from the `common` JAR at compile/runtime. No import changes are needed.

### Parent POM `dependencyManagement` example

```xml
<dependencyManagement>
  <dependencies>
    <!-- spring-boot-dependencies BOM already inherited from spring-boot-starter-parent -->
    <dependency>
      <groupId>com.anasdidi</groupId>
      <artifactId>uam-common</artifactId>
      <version>${project.version}</version>
    </dependency>
  </dependencies>
</dependencyManagement>
```

This allows `uam/pom.xml` to declare the common dependency without an explicit version.

## Sub-Tasks

### Sub-Task 1: Create parent POM at `app/pom.xml`

- **Status:** Pending
- **Objective:** Create a Maven parent POM at the `app/` level that acts as the multi-module aggregator and inherits `spring-boot-starter-parent`.
- **Related Requirements:** R1, R2
- **Dependencies and Preconditions:** None.
- **In Scope for This Sub-Task:**
  - Create `app/pom.xml` with `<packaging>pom</packaging>`.
  - Set parent to `spring-boot-starter-parent:4.0.6`.
  - Set `groupId=com.anasdidi`, `artifactId=app`, `version=0.0.1-SNAPSHOT`.
  - Define `<properties>` with `<java.version>25</java.version>`.
  - Declare `<modules>`: `common` then `uam` (order matters — common first).
  - Define `<dependencyManagement>` to manage `com.anasdidi:uam-common` version.
  - Define `<pluginManagement>` for `spring-boot-maven-plugin` (so it's not inherited automatically by common).
  - Define Spotless plugin in `<build><plugins>` (inherited by all modules) with the same config currently in `uam/pom.xml` — Java (palantirJavaFormat, GOOGLE style, formatAnnotations, removeUnusedImports), POM (sortPom with 2-space indent), and XML resource files (Eclipse WTP). Bind `spotless:check` to the `verify` phase.
- **Out of Scope for This Sub-Task:**
  - No `dependencies` (only `dependencyManagement`).
  - No other build plugins beyond Spotless and pluginManagement entries.
- **Instructions:**
  - Create file `/home/vscode/workspace/app/pom.xml`.
  - Content must include: parent declaration, group/artifact/version/packaging, properties, modules, dependencyManagement, pluginManagement.
- **Acceptance Criteria:**
  - `app/pom.xml` exists and `./mvnw validate` runs without error from `app/` directory.
- **Implementation Suggestions:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.6</version>
    <relativePath/>
  </parent>

  <groupId>com.anasdidi</groupId>
  <artifactId>app</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <packaging>pom</packaging>

  <properties>
    <java.version>25</java.version>
  </properties>

  <modules>
    <module>common</module>
    <module>uam</module>
  </modules>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>com.anasdidi</groupId>
        <artifactId>uam-common</artifactId>
        <version>${project.version}</version>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
      </plugins>
    </pluginManagement>
    <plugins>
      <!--
        Code formatting with Spotless (inherited by all modules):
          mvn spotless:check   - verify formatting (also runs during verify phase)
          mvn spotless:apply   - auto-format all files
        Uses palantirJavaFormat with style=GOOGLE (4-space indent), 2-space for POM,
        and Eclipse WTP (4.21.0) for XML resource files.
      -->
      <plugin>
        <groupId>com.diffplug.spotless</groupId>
        <artifactId>spotless-maven-plugin</artifactId>
        <version>2.44.3</version>
        <configuration>
          <java>
            <palantirJavaFormat>
              <version>2.89.0</version>
              <style>GOOGLE</style>
            </palantirJavaFormat>
            <formatAnnotations/>
            <removeUnusedImports/>
          </java>
          <pom>
            <sortPom>
              <nrOfIndentSpace>2</nrOfIndentSpace>
            </sortPom>
          </pom>
          <formats>
            <format>
              <includes>
                <include>src/**/resources/**/*.xml</include>
                <include>src/**/resources/**/*.xsd</include>
              </includes>
              <eclipseWtp>
                <type>XML</type>
                <version>4.21.0</version>
              </eclipseWtp>
            </format>
          </formats>
        </configuration>
        <executions>
          <execution>
            <goals>
              <goal>check</goal>
            </goals>
            <phase>verify</phase>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

- **Testing Suggestions:** Run `./mvnw validate` from `app/`.

### Sub-Task 2: Create `common` module and move common sources

- **Status:** Pending
- **Objective:** Create `app/common/` as a Maven module with its own POM, and move the `com.anasdidi.uam.common` package from `app/uam/src/` into it.
- **Related Requirements:** R1, R2, R3
- **Dependencies and Preconditions:** Sub-Task 1 (parent POM must exist).
- **In Scope for This Sub-Task:**
  - Create `app/common/pom.xml` inheriting from `app/pom.xml`.
  - Set `artifactId=uam-common`.
  - Add dependencies: `spring-web` (for `HttpStatus` in `ResponseEnum`), `lombok` (optional).
  - Create package directory structure: `app/common/src/main/java/com/anasdidi/uam/common/` and `app/common/src/main/java/com/anasdidi/uam/common/enums/`.
  - Copy (then delete from uam) these 4 files:
    - `CommonConstants.java`
    - `IBaseReqDTO.java`
    - `IBaseResDTO.java`
    - `enums/ResponseEnum.java`
  - Verify the files compile as part of the `common` module.
- **Out of Scope for This Sub-Task:**
  - No Spring Boot plugin in common POM.
  - No test files to move (there are no common-specific tests).
  - No changes to imports (covered in Sub-Task 3).
- **Instructions:**
  - Create the directory structure.
  - Create `common/pom.xml`.
  - Copy (Write) the 4 Java files from their current uam location to the equivalent path under `app/common/src/main/java/`.
  - Delete the originals from `app/uam/src/main/java/com/anasdidi/uam/common/` (use bash to remove the entire `common/` directory and its `enums/` subdirectory from uam).
- **Acceptance Criteria:**
  - `app/common/src/main/java/com/anasdidi/uam/common/CommonConstants.java` exists.
  - `app/common/src/main/java/com/anasdidi/uam/common/IBaseReqDTO.java` exists.
  - `app/common/src/main/java/com/anasdidi/uam/common/IBaseResDTO.java` exists.
  - `app/common/src/main/java/com/anasdidi/uam/common/enums/ResponseEnum.java` exists.
  - `app/uam/src/main/java/com/anasdidi/uam/common/` no longer exists.
  - `./mvnw compile -pl common` succeeds from `app/`.
- **Cautionary Points:**
  - `ResponseEnum` depends on `org.springframework.http.HttpStatus` — must add `spring-web` dependency.
  - Lombok is annotation-only, set `<optional>true</optional>`.
  - Do NOT use `spring-boot-starter-web` (too heavy); just `spring-web`.
  - Keep package as `com.anasdidi.uam.common` (NOT `com.anasdidi.common`).
- **Implementation Suggestions for `common/pom.xml`:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.anasdidi</groupId>
    <artifactId>app</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
  </parent>

  <artifactId>uam-common</artifactId>

  <dependencies>
    <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>
  </dependencies>
</project>
```

### Sub-Task 3: Update `uam/pom.xml` — change parent and add common dependency

- **Status:** Pending
- **Objective:** Update `app/uam/pom.xml` to inherit from `app/pom.xml` instead of `spring-boot-starter-parent`, add `uam-common` as a dependency, and keep `spring-boot-maven-plugin` in its build section.
- **Related Requirements:** R1, R2
- **Dependencies and Preconditions:** Sub-Task 1 (parent POM exists), Sub-Task 2 (common module delivers the artifact).
- **In Scope for This Sub-Task:**
  - Change parent from `org.springframework.boot:spring-boot-starter-parent` to `com.anasdidi:app`.
  - Add `<relativePath>../pom.xml</relativePath>` in the parent declaration.
  - Remove `groupId` (inherited), remove `version` (inherited).
  - Add `com.anasdidi:uam-common` to `<dependencies>` (version managed by parent's `<dependencyManagement>`).
  - Keep `spring-boot-maven-plugin` in `<build><plugins>` (now activated from `<pluginManagement>` in parent).
  - Keep the `maven-compiler-plugin` annotation processor config for Lombok (it is still needed for `uam` sources).
  - **Remove** the Spotless plugin configuration (now inherited from parent POM).
- **Out of Scope for This Sub-Task:**
  - No other dependency changes.
  - No removal of any existing dependency.
- **Instructions:**
  - Edit `app/uam/pom.xml`:
    - Change parent to `com.anasdidi:app` with `../pom.xml` relative path.
    - Remove `<groupId>` (inherited from parent) and `<version>` (inherited from parent).
    - Add `<dependency>` for `com.anasdidi:uam-common`.
    - Keep `url`, `licenses`, `developers`, `scm` (can be removed or kept).
    - Remove `<properties><java.version>25</java.version></properties>` (inherited from parent).
    - The `spring-boot-maven-plugin` is already in `<build><plugins>` — keep it as-is (it will use the `<pluginManagement>` config from parent).
    - **Remove** the Spotless plugin from `<build><plugins>` (it now lives in the parent POM and applies to all modules automatically).
  - Run `./mvnw compile -pl uam -am` from `app/` to verify.
- **Acceptance Criteria:**
  - `./mvnw compile -pl uam -am` succeeds from `app/` (builds common first, then uam).
  - `./mvnw test -pl uam -am` succeeds from `app/` (all existing tests pass).
  - No change to the application startup behavior.

### Sub-Task 4: Final integration verification

- **Status:** Pending
- **Objective:** Run the full build pipeline to verify everything works end-to-end.
- **Related Requirements:** R1, R2, R3
- **Dependencies and Preconditions:** All sub-tasks 1–3 completed.
- **In Scope for This Sub-Task:**
  - Run `./mvnw clean compile -pl uam -am` from `app/`.
  - Run `./mvnw test -pl uam -am` from `app/`.
  - Run `./mvnw spotless:check` from `app/` (format check on all modules — inherited from parent POM).
  - Optionally run `./mvnw verify` from `app/` for full pipeline (all modules).
- **Out of Scope for This Sub-Task:**
  - No code changes.
- **Acceptance Criteria:**
  - Full build: compilation, all tests, and format checks pass.
  - The `common` JAR (`uam-common-0.0.1-SNAPSHOT.jar`) is produced in `app/common/target/`.

## Final Integration & Verification

- **System-Wide Test:**
  ```bash
  cd /home/vscode/workspace/app
  ./mvnw clean verify
  ```
  Expected: BUILD SUCCESS, all tests pass, spotless check passes for all modules.

- **Completion Checklist:**
  - [ ] `app/pom.xml` exists and is valid.
  - [ ] `app/common/pom.xml` exists and correctly inherits from parent.
  - [ ] `app/uam/pom.xml` inherits from `app/pom.xml`, depends on `uam-common`.
  - [ ] All `com.anasdidi.uam.common.*` classes moved to `app/common/src/main/java/`.
  - [ ] No remaining `com/anasdidi/uam/common/` directory under `app/uam/src/`.
  - [ ] `./mvnw compile` succeeds for both modules.
  - [ ] `./mvnw test` passes for both modules.
  - [ ] `./mvnw spotless:check` passes for all modules.
  - [ ] All existing Java imports in `uam` remain unchanged and compile correctly.

## Open Questions

- None. The scope and approach are well-defined.
