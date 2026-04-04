# Upgrade Plan: supplytracker (20260403233625)

- **Generated**: 2026-04-03 23:36:25
- **HEAD Branch**: main
- **HEAD Commit ID**: 6dae5c6

## Available Tools

**JDKs**
- JDK 17.0.11: `C:\Program Files\Java\jdk-17\bin` (current project JDK, used by steps 2)
- JDK 21: **<TO_BE_INSTALLED>** (required by steps 3–4)

**Build Tools**
- Maven 3.9.9: `C:\Program Files\Maven\apache-maven-3.9.9\bin` (compatible with Java 21, no upgrade needed)
- No Maven Wrapper present

## Guidelines

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred. <!-- this note is for users, NEVER remove it -->

## Options

- Working branch: appmod/java-upgrade-20260403233625
- Run tests before and after the upgrade: true

## Upgrade Goals

- Upgrade Java from 17 to 21 (latest LTS as of April 2026)

### Technology Stack

<!--
  Table of core dependencies and their compatibility with upgrade goals.
  IMPORTANT: Analyze ALL modules in multi-module projects, not just the root module.
  Only include: direct dependencies + those critical for upgrade compatibility.
  CRITICAL: Identify and clearly flag EOL (End of Life) dependencies - these pose security risks and must be upgraded.

  Columns:
  - Technology/Dependency: Name of the dependency (mark EOL dependencies with "⚠️ EOL" suffix)
  - Current: Version currently in use
  - Min Compatible Version: Minimum version that works with upgrade goals (or N/A if replaced)
  - Why Incompatible: Explanation of incompatibility, or "-" if already compatible. For EOL deps, mention security/support concerns.

  IMPORTANT: Include build tools (Maven/Gradle), wrappers, and key build plugins in this table.
  Build tools and plugins are upgrade-critical even though they are not runtime dependencies.

  SAMPLE:
  | Technology/Dependency    | Current | Min Compatible | Why Incompatible                               |
  | ------------------------ | ------- | -------------- | ---------------------------------------------- |
  | Java                     | 8       | 21             | User requested                                 |
  | Spring Boot              | 2.5.0   | 3.2.0          | User requested                                 |
  | Spring Framework         | 5.3.x   | 6.1.x          | Spring Boot 3.2 requires Spring Framework 6.1+ |
  | Maven (wrapper)          | 3.6.3   | 3.9.0          | Maven 3.6.x does not support Java 21           |
  | maven-compiler-plugin    | 3.8.1   | 3.11.0         | Older versions cannot compile Java 21 bytecode |
  | maven-surefire-plugin    | 2.22.2  | 3.1.0          | Older versions may fail with Java 17+ module system |
  | Hibernate                | 5.4.x   | 6.1.x          | Spring Boot 3.x requires Hibernate 6+          |
  | javax.servlet ⚠️ EOL     | 4.0     | N/A            | Replaced by jakarta.servlet in Spring Boot 3.x; javax namespace EOL |
  | Log4j ⚠️ EOL             | 1.2.17  | N/A            | EOL since 2015, critical security vulnerabilities; replace with Logback/Log4j2 |
  | DWR ⚠️ EOL             | 3.0.1.rc  | N/A            | EOL since 2017, no longer maintained; consider replacing with modern alternatives |
  | Lombok                   | 1.18.20 | 1.18.20        | -                                              |
-->

| Technology/Dependency    | Current             | Min Compatible | Why Incompatible |
| ------------------------ | ------------------- | -------------- | ---------------- |
| Java                     | 17                  | 21             | User requested upgrade to latest LTS |
| Spring Boot              | 3.1.1               | 3.2.0          | 3.1.x predates Java 21 GA; 3.2+ has certified Java 21 support |
| spring-boot-maven-plugin | 3.1.1               | 3.2.0          | Must match Spring Boot parent version |
| JJWT                     | 0.11.5              | 0.11.5         | Compatible with Java 21 - no change needed |
| AWS SDK v2               | 2.28.29             | 2.28.29        | Compatible with Java 21 - no change needed |
| Lombok                   | 1.18.28 (managed)   | 1.18.26        | Managed by Spring Boot BOM, compatible with Java 21 |
| Maven                    | 3.9.9               | 3.9.0          | Compatible - no change needed |
| Dockerfile base image    | eclipse-temurin:17  | eclipse-temurin:21 | Must reflect target JDK |

### Derived Upgrades

- Upgrade Spring Boot from 3.1.1 to 3.3.6 (3.1.x predates Java 21 GA; 3.3.x is the latest maintenance patch line with certified Java 21 support)
- Update `spring-boot-maven-plugin` from 3.1.1 to 3.3.6 (must match Spring Boot parent version)
- Add `<java.version>21</java.version>` property to `pom.xml` (spring-boot-starter-parent uses this to configure the maven-compiler-plugin source/target)
- Update Dockerfile: build image `maven:3.9-eclipse-temurin-17` → `maven:3.9-eclipse-temurin-21`; runtime image `eclipse-temurin:17-jre-alpine` → `eclipse-temurin:21-jre-alpine`

## Upgrade Steps

- **Step 1: Setup Environment**
  - **Rationale**: JDK 21 is not installed on the system. Install it so subsequent steps can compile and run with the target Java version.
  - **Changes to Make**:
    - [ ] Install JDK 21 via `#appmod-install-jdk`
  - **Verification**:
    - Command: `#appmod-list-jdks` to confirm JDK 21 is available
    - Expected: JDK 21 listed with a valid path

---

- **Step 2: Setup Baseline**
  - **Rationale**: Establish pre-upgrade compilation and test results with JDK 17 + Spring Boot 3.1.1 to define the acceptance baseline.
  - **Changes to Make**:
    - [ ] Run full compile (main + test sources) with JDK 17
    - [ ] Run test suite with JDK 17
  - **Verification**:
    - Command: `mvn clean test-compile && mvn clean test` with `JAVA_HOME=C:\Program Files\Java\jdk-17`
    - Expected: Document SUCCESS/FAILURE and test pass/fail count (baseline)

---

- **Step 3: Upgrade Spring Boot to 3.3.6 and Set Java 21**
  - **Rationale**: Spring Boot 3.1.x predates Java 21 GA; upgrading to 3.3.6 brings certified Java 21 support. Setting `java.version=21` configures the compiler. Updating the Dockerfile aligns the container runtime.
  - **Changes to Make**:
    - [ ] Update `spring-boot-starter-parent` version from `3.1.1` to `3.3.6` in `pom.xml`
    - [ ] Update `spring-boot-maven-plugin` version from `3.1.1` to `3.3.6` in `pom.xml`
    - [ ] Add `<java.version>21</java.version>` to the `<properties>` section of `pom.xml`
    - [ ] Update Dockerfile: `maven:3.9-eclipse-temurin-17` → `maven:3.9-eclipse-temurin-21`; `eclipse-temurin:17-jre-alpine` → `eclipse-temurin:21-jre-alpine`
  - **Verification**:
    - Command: `mvn clean test-compile` with `JAVA_HOME=<JDK 21 path>`
    - Expected: Compilation SUCCESS (tests may fail - fixed in Final Validation)

---

- **Step 4: Final Validation**
  - **Rationale**: Verify all upgrade goals are met (Java 21 + Spring Boot 3.3.6), compilation succeeds with JDK 21, and test pass rate meets or exceeds baseline.
  - **Changes to Make**:
    - [ ] Confirm `java.version=21` and `spring-boot-starter-parent 3.3.6` in `pom.xml`
    - [ ] Resolve any remaining compilation errors from Spring Boot BOM version changes
    - [ ] Run full test suite; fix all failures iteratively until 100% pass (or >= baseline)
  - **Verification**:
    - Command: `mvn clean test` with `JAVA_HOME=<JDK 21 path>`
    - Expected: Compilation SUCCESS + all tests pass (>= baseline)

## Key Challenges

- **Spring Boot 3.1.x → 3.3.x Managed Dependency Version Changes**
  - **Challenge**: Spring Boot BOM manages many transitive dependency versions. Upgrading from 3.1.1 to 3.3.6 bumps Jackson, Lettuce, Micrometer, and other managed deps automatically.
  - **Strategy**: This project has no test files; compilation is the primary gate. Review any explicit version overrides in pom.xml that may conflict with the new BOM.

- **Java 21 Module System and Reflection**
  - **Challenge**: Java 21 strengthens module encapsulation. Libraries using deep reflection (e.g., AWS SDK internals) may require `--add-opens` JVM flags.
  - **Strategy**: Run the full build with JDK 21; if `InaccessibleObjectException` appears, add the required JVM args to the Maven Surefire plugin configuration or `application.properties`.

## Plan Review

Plan is feasible and complete. No unfixable limitations identified. The upgrade from Java 17 to 21 via Spring Boot 3.3.6 is a well-established, low-risk path. The project has no test files so the test baseline will show 0 tests, and Final Validation will pass on the test front. The primary risk is compilation errors from Spring Boot BOM managed dependency version bumps, which will be resolved iteratively in Step 3.
