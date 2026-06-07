# Plan: Centralize Common Dependencies in Parent POM

## Objective

Move hardcoded dependency versions from child modules into the parent POM's `<dependencyManagement>` (with a Maven property) and consolidate duplicated build plugin configuration (Lombok annotation processor path) into the parent POM's `<pluginManagement>`. This ensures all dependency versions and plugin configs are managed in one place across the multi-module project.

## Requirements Snapshot

- **R1:** Move `org.springdoc:springdoc-openapi-starter-webflux-ui` version `3.0.2` from `uam/pom.xml` into parent POM `<dependencyManagement>` with a `${springdoc.version}` property.
- **R2:** Consolidate the duplicated Lombok `annotationProcessorPaths` config from `common/pom.xml` and `uam/pom.xml` into parent POM `<pluginManagement>` so child modules inherit it without repeating it.

## Scope

- **In scope:**
  - Add `<springdoc.version>` property to `app/pom.xml`.
  - Add `org.springdoc:springdoc-openapi-starter-webflux-ui` entry to parent `<dependencyManagement>` referencing `${springdoc.version}`.
  - Remove inline `<version>3.0.2</version>` from `uam/pom.xml` for the springdoc dependency.
  - Add `maven-compiler-plugin` with Lombok annotation processor config to parent `<pluginManagement>`.
  - Remove the explicit `maven-compiler-plugin` / `annotationProcessorPaths` block from `common/pom.xml` (now inherited).
  - Remove the explicit `maven-compiler-plugin` / `annotationProcessorPaths` block from `uam/pom.xml` (now inherited).
  - Run full build verification (`./mvnw clean verify`).

- **Out of scope:**
  - No changes to any other dependencies (all other versions are already managed by Spring Boot BOM).
  - No changes to business logic, source code, or tests.
  - No changes to Spotless or other existing parent POM configurations.
  - No changes to plugin versions (already managed by Spring Boot BOM for `maven-compiler-plugin`).

## Assumptions and Constraints

- Parent POM already inherits `spring-boot-starter-parent:4.0.6`, which manages `maven-compiler-plugin` version.
- `common/pom.xml` and `uam/pom.xml` both currently declare their own `maven-compiler-plugin` with Lombok `annotationProcessorPaths`.
- The `uam/pom.xml` has a separate `<execution>` block (default-compile and default-testCompile) for the annotation processor — this must be preserved exactly in form, only moved to parent `<pluginManagement>`.
- `common/pom.xml` has a simpler single `<configuration><annotationProcessorPaths>` block.
- After consolidation, a child module only needs `<plugin>` declaration (no config body) to inherit the annotation processor setup, **unless** it needs to override any config (none do).

## Risks and Areas Requiring Care

- **Execution ID conflicts:** The parent `<pluginManagement>` `maven-compiler-plugin` config must include the `executions` block. If `uam/pom.xml` only declares `<plugin><artifactId>maven-compiler-plugin</artifactId></plugin>` (empty), it will inherit the parent's executions. Verify this.
- **`common` module does NOT have executions** — its annotation processor is a simple `<configuration>` block. The parent config must use the `executions` structure (to satisfy `uam`), and `common` must verify it inherits correctly.
- **Pre-existing plan.md:** The file will be overwritten with this new plan. That is intentional — the old multi-module refactor plan is complete.
- **Spring Boot 4.x:** The `maven-compiler-plugin` is managed by `spring-boot-starter-parent` — no version needed in `<pluginManagement>`.

## Core Concepts

### How `<pluginManagement>` Consolidation Works

**Before** — each child declares its own plugin config:

```xml
<!-- common/pom.xml -->
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <configuration>
        <annotationProcessorPaths>
          <path>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
          </path>
        </annotationProcessorPaths>
      </configuration>
    </plugin>
  </plugins>
</build>
```

```xml
<!-- uam/pom.xml -->
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <executions>
        <execution>
          <id>default-compile</id>
          <goals><goal>compile</goal></goals>
          <phase>compile</phase>
          <configuration>
            <annotationProcessorPaths>
              <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
              </path>
            </annotationProcessorPaths>
          </configuration>
        </execution>
        <execution>
          <id>default-testCompile</id>
          <goals><goal>testCompile</goal></goals>
          <phase>test-compile</phase>
          <configuration>
            <annotationProcessorPaths>
              <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
              </path>
            </annotationProcessorPaths>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

**After** — parent `<pluginManagement>` contains the full config; children declare only the plugin reference:

```xml
<!-- parent pom.xml — in <pluginManagement> -->
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <executions>
    <execution>
      <id>default-compile</id>
      <goals><goal>compile</goal></goals>
      <phase>compile</phase>
      <configuration>
        <annotationProcessorPaths>
          <path>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
          </path>
        </annotationProcessorPaths>
      </configuration>
    </execution>
    <execution>
      <id>default-testCompile</id>
      <goals><goal>testCompile</goal></goals>
      <phase>test-compile</phase>
      <configuration>
        <annotationProcessorPaths>
          <path>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
          </path>
        </annotationProcessorPaths>
      </configuration>
    </execution>
  </executions>
</plugin>
```

```xml
<!-- common/pom.xml — simplified -->
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
    </plugin>
  </plugins>
</build>
```

```xml
<!-- uam/pom.xml — simplified -->
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
    </plugin>
  </plugins>
</build>
```

**Important subtlety:** The parent config uses an `executions` block. For `common`, which previously had only a `<configuration>` block (no `<executions>`), the inherited executions will apply the annotation processor to both compile and testCompile phases automatically. This is **strictly better** — previously, `common` didn't explicitly configure annotation processing for testCompile, which was a latent gap. This is safe because `common` has no tests yet; adding annotation processing for testCompile has no negative effect.

### How `dependencyManagement` Works

**Before in `uam/pom.xml`:**
```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
  <version>3.0.2</version>
</dependency>
```

**After in `app/pom.xml` `<dependencyManagement>`:**
```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
  <version>${springdoc.version}</version>
</dependency>
```

**After in `uam/pom.xml`:**
```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
</dependency>
```

## Sub-Tasks

### Sub-Task 1: Add springdoc version property and managed dependency to parent POM

- **Status:** Pending
- **Objective:** Add `<springdoc.version>` property and `org.springdoc` entry to parent `<dependencyManagement>`.
- **Related Requirements:** R1
- **Dependencies and Preconditions:** Parent POM exists at `app/pom.xml` (already does).
- **In Scope for This Sub-Task:**
  - Add `<springdoc.version>3.0.2</springdoc.version>` to `<properties>` in `app/pom.xml`.
  - Add `<dependency>` for `org.springdoc:springdoc-openapi-starter-webflux-ui` inside parent `<dependencyManagement>` with version `${springdoc.version}`.
- **Out of Scope for This Sub-Task:**
  - No changes to any child POM (done in Sub-Task 2).
  - No other property additions.
- **Instructions:**
  - Edit `/home/vscode/workspace/app/pom.xml`.
  - Insert the property and the managed dependency entry.
- **Acceptance Criteria:**
  - `./mvnw validate` from `app/` succeeds.
- **Implementation Suggestions:**
  ```xml
  <!-- Within <properties> block, add: -->
  <springdoc.version>3.0.2</springdoc.version>

  <!-- Within <dependencyManagement><dependencies>, after the common entry, add: -->
  <dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
    <version>${springdoc.version}</version>
  </dependency>
  ```
- **Cautionary Points:**
  - Spring Boot does NOT manage `springdoc`, so the version must be explicitly set in `<dependencyManagement>`. Currently it's in the child; this sub-task just moves it to the parent.
- **Testing Suggestions:** Run `./mvnw validate -pl uam -am` from `app/`.

### Sub-Task 2: Remove inline springdoc version from `uam/pom.xml`

- **Status:** Pending
- **Objective:** Remove the hardcoded `version` from the `springdoc` dependency in `uam/pom.xml` so it is inherited from parent `<dependencyManagement>`.
- **Related Requirements:** R1
- **Dependencies and Preconditions:** Sub-Task 1 (parent must manage the version).
- **In Scope for This Sub-Task:**
  - Remove `<version>3.0.2</version>` from the `org.springdoc:springdoc-openapi-starter-webflux-ui` dependency entry in `uam/pom.xml`.
- **Out of Scope for This Sub-Task:**
  - No other changes to `uam/pom.xml`.
- **Instructions:**
  - Edit `/home/vscode/workspace/app/uam/pom.xml`.
  - Remove the `<version>3.0.2</version>` line from the springdoc dependency block.
- **Acceptance Criteria:**
  - The springdoc dependency in `uam/pom.xml` has no `<version>` child.
  - `./mvnw compile -pl uam -am` succeeds from `app/`.
- **Testing Suggestions:** Run `./mvnw compile -pl uam -am` from `app/`.

### Sub-Task 3: Consolidate Lombok annotation processor config into parent `<pluginManagement>`

- **Status:** Pending
- **Objective:** Move the duplicated `maven-compiler-plugin` annotation processor configuration from `common/pom.xml` and `uam/pom.xml` into the parent POM's `<pluginManagement>` so both modules inherit it.
- **Related Requirements:** R2
- **Dependencies and Preconditions:** Sub-Tasks 1–2 (can run in parallel, but order doesn't matter).
- **In Scope for This Sub-Task:**
  - Add `maven-compiler-plugin` with `executions` for `default-compile` and `default-testCompile`, each with Lombok `annotationProcessorPaths`, to parent POM's `<pluginManagement>` section.
  - Replace the explicit `maven-compiler-plugin` block in `common/pom.xml` with a minimal `<plugin>` reference (no `<configuration>`, no `<executions>`).
  - Replace the explicit `maven-compiler-plugin` block in `uam/pom.xml` with a minimal `<plugin>` reference (no `<configuration>`, no `<executions>`).
- **Out of Scope for This Sub-Task:**
  - No changes to `spring-boot-maven-plugin` or any other plugin.
  - No changes to Spotless plugin.
- **Instructions:**
  - **Step 1:** Edit `/home/vscode/workspace/app/pom.xml`. In `<pluginManagement><plugins>` (inside `<build>`), add the `maven-compiler-plugin` entry with both executions (`default-compile` and `default-testCompile`) containing the Lombok annotation processor path.
  - **Step 2:** Edit `/home/vscode/workspace/app/common/pom.xml`. Replace the full `maven-compiler-plugin` block with just:
    ```xml
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
    </plugin>
    ```
  - **Step 3:** Edit `/home/vscode/workspace/app/uam/pom.xml`. Replace the full `maven-compiler-plugin` block with just:
    ```xml
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
    </plugin>
    ```
  - Verify with compilation.
- **Acceptance Criteria:**
  - Parent POM's `<pluginManagement>` contains `maven-compiler-plugin` with both Lombok executions.
  - `common/pom.xml` has only a bare `<plugin>` reference for `maven-compiler-plugin`.
  - `uam/pom.xml` has only a bare `<plugin>` reference for `maven-compiler-plugin`.
  - `./mvnw compile -pl common` succeeds (Lombok annotation processing works for common).
  - `./mvnw compile -pl uam -am` succeeds (Lombok annotation processing works for uam).
- **Cautionary Points:**
  - The `uam/pom.xml` executions explicitly set `id=default-compile` and `id=default-testCompile`, which override the plugin's default bindings. When moved to parent `<pluginManagement>`, they must remain as `executions` (not a bare `<configuration>`) to apply correctly to both phases.
  - For `common`, which previously had only a `<configuration>` block (no executions), the inherited executions will now apply Lombok to both compile and testCompile. This may cause Spotless format check to flag the POM after `sortPom` reformats it.
- **Implementation Suggestion for `app/pom.xml` `<pluginManagement>` addition:**
  ```xml
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <executions>
      <execution>
        <id>default-compile</id>
        <goals><goal>compile</goal></goals>
        <phase>compile</phase>
        <configuration>
          <annotationProcessorPaths>
            <path>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
            </path>
          </annotationProcessorPaths>
        </configuration>
      </execution>
      <execution>
        <id>default-testCompile</id>
        <goals><goal>testCompile</goal></goals>
        <phase>test-compile</phase>
        <configuration>
          <annotationProcessorPaths>
            <path>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
            </path>
          </annotationProcessorPaths>
        </configuration>
      </execution>
    </executions>
  </plugin>
  ```
- **Testing Suggestions:**
  ```bash
  cd /home/vscode/workspace/app
  ./mvnw compile -pl common
  ./mvnw compile -pl uam -am
  ./mvnw test -pl uam -am
  ./mvnw spotless:check
  ```

### Sub-Task 4: Final integration verification

- **Status:** Pending
- **Objective:** Run the full build pipeline to verify everything works end-to-end after all changes.
- **Related Requirements:** R1, R2
- **Dependencies and Preconditions:** Sub-Tasks 1, 2, and 3 completed.
- **In Scope for This Sub-Task:**
  - Run `./mvnw clean compile -pl common` from `app/`.
  - Run `./mvnw compile -pl uam -am` from `app/`.
  - Run `./mvnw test -pl uam -am` from `app/`.
  - Run `./mvnw spotless:check` from `app/`.
  - Run `./mvnw clean verify` from `app/` for full pipeline.
- **Out of Scope for This Sub-Task:**
  - No code changes.
- **Acceptance Criteria:**
  - Full build: compilation, all tests, and format checks pass.
  - Spotless/`sortPom` has sorted all POM files correctly (format check passes).
  - The `springdoc` artifact resolves correctly at the managed version.
  - Lombok annotations are processed correctly in both modules (verified by compilation).

## Final Integration & Verification

- **System-Wide Test:**
  ```bash
  cd /home/vscode/workspace/app
  ./mvnw clean verify
  ```
  Expected: BUILD SUCCESS, all tests pass, spotless check passes for all modules.

- **Completion Checklist:**
  - [ ] `app/pom.xml` has `<springdoc.version>3.0.2</springdoc.version>` property.
  - [ ] `app/pom.xml` has `org.springdoc:springdoc-openapi-starter-webflux-ui` in `<dependencyManagement>`.
  - [ ] `app/pom.xml` has `maven-compiler-plugin` with Lombok executions in `<pluginManagement>`.
  - [ ] `common/pom.xml` springdoc dependency has no `<version>` (only in parent management).
  - [ ] `uam/pom.xml` springdoc dependency has no `<version>` (only in parent management).
  - [ ] `common/pom.xml` has a bare `<plugin>` for `maven-compiler-plugin` (no config).
  - [ ] `uam/pom.xml` has a bare `<plugin>` for `maven-compiler-plugin` (no config).
  - [ ] `./mvnw compile -pl common` succeeds.
  - [ ] `./mvnw compile -pl uam -am` succeeds.
  - [ ] `./mvnw test -pl uam -am` succeeds.
  - [ ] `./mvnw spotless:check` passes.
  - [ ] `./mvnw clean verify` passes (BUILD SUCCESS).

## Open Questions

- None. The scope and approach are well-defined from the dependency scan results.
