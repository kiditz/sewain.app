# Backend code coverage

JaCoCo 0.8.14 produces HTML and XML coverage reports for the default suite,
PostgreSQL integration suite and both suites combined. All three reports use the
same application-code scope defined by `coverageClasses` in `backend/build.gradle`.

## Run and open reports

Use JDK 21. From `backend/`:

```sh
# Default suite; no Docker required. Also generates its coverage report.
./gradlew test
open build/reports/jacoco/test/html/index.html

# PostgreSQL integration suite; Docker must be running.
./gradlew integrationTest
open build/reports/jacoco/integrationTest/html/index.html

# Both suites and combined coverage; Docker required.
./gradlew jacocoCombinedReport
open build/reports/jacoco/combined/html/index.html

# Force fresh execution of both suites instead of accepting up-to-date results.
./gradlew jacocoCombinedReport --rerun-tasks
```

`open` is the macOS command; on other platforms, open the HTML file in a browser.
Integration tests create temporary PostgreSQL containers. Application containers
and the development database do not need to be running.

`jacocoTestReport` and `jacocoIntegrationTestReport` can also be invoked directly;
each depends on its corresponding test task. `test` and `integrationTest` finalize
with their own reports. The standard `test`, `check` and `build` lifecycle does not
implicitly execute `integrationTest`.

## Outputs

Paths below are relative to `backend/`.

| Suite | HTML | XML |
| --- | --- | --- |
| Default | `build/reports/jacoco/test/html/index.html` | `build/reports/jacoco/test/jacoco.xml` |
| Integration | `build/reports/jacoco/integrationTest/html/index.html` | `build/reports/jacoco/integrationTest/jacoco.xml` |
| Combined | `build/reports/jacoco/combined/html/index.html` | `build/reports/jacoco/combined/jacoco.xml` |

JUnit HTML results are under `build/reports/tests/test/` and
`build/reports/tests/integrationTest/`. JUnit XML results are under
`build/test-results/test/` and `build/test-results/integrationTest/`.

Execution data is stored separately in `build/jacoco/test.exec` and
`build/jacoco/integrationTest.exec`. Combined coverage merges observations of the
same production code; it does not add or average coverage percentages. It remains
useful for persistence and other paths exercised only by integration tests.

## Measured scope and generated code

The shared scope includes service, calculator, validator and specification class
patterns, plus selected search, content-security, pagination, integration-access,
secret, email-transport, WhatsApp-gateway, media-storage/download/upload and finance
event-listener classes. The exact include patterns in `coverageClasses` are the
source of truth; new application logic outside those patterns must be reviewed
for inclusion.

Entities, generated mappers, DTOs, controllers, configuration and other classes
outside that allowlist are not measured by these reports. They can still be used
or verified by tests. These percentages therefore describe the configured scope,
not every production class or every application behavior.

Hibernate enhancement adds methods such as `$$_hibernate_write_*` to entity bytecode. Those methods are framework-generated, not business
rules requiring dedicated unit tests. Entity classes are outside the measured
scope; this is class-level scoping, not a method-level Hibernate filter. A missing
debug-information warning on generated methods does not mean authored source must
be tested through Hibernate internals.

## Coverage verification

There is one opt-in gate using the same scope:

```sh
./gradlew jacocoTestCoverageVerification
```

It requires **100% line and 100% branch coverage from the default suite only**.
It is not attached to `test`, `check` or `build`, and it does not use integration
execution data. A successful test run or combined report does not imply this gate
passes. The previous separate business-report/verification tasks have been
removed; business unit tests run under the standard `test` task.

## Latest verified results

Verified locally on **2026-11-14**: 422 default-suite tests and 43 PostgreSQL
integration tests passed, with zero failures, errors or skips. The default suite
ran unfiltered; integration tests were explicitly rerun. All three reports were
regenerated. `git diff --check` passed.

| Report | Covered / total lines | Line coverage | Covered / total branches | Branch coverage |
| --- | ---: | ---: | ---: | ---: |
| Default | 3,354 / 3,422 | 98.01% | 1,781 / 2,093 | 85.09% |
| Integration | 946 / 3,422 | 27.64% | 380 / 2,093 | 18.16% |
| Combined | 3,379 / 3,422 | 98.74% | 1,789 / 2,093 | 85.48% |

The 100% target has not been reached: the combined report still contains 43 missed
lines and 304 missed branches. These figures are a dated snapshot, not a guarantee
for later commits or environments. No thresholds or scope patterns were relaxed
for this run.

## Reading reports correctly

- A covered line does not prove all decisions or failure paths were tested. Review
  branch coverage and assertions as well as the headline percentage.
- The default suite includes unit tests and lightweight HTTP, filesystem and
  architecture checks; it is not exclusively isolated unit tests.
- A filtered command such as `./gradlew test --tests '*CountryQueryServiceTest'`
  replaces that suite's execution data with coverage for the selection. Run the
  full suite again before interpreting default or combined coverage as complete.
- Avoid combining execution data from different production builds. Rerun both
  suites after production-code or compiler changes.
- Finalizers may generate partial reports after a failure. Compilation or JVM
  startup failures can leave older reports in place. Check task outcomes and
  timestamps before interpreting coverage.
- Prefer behavior-focused tests. Unit tests cover decisions and edge cases;
  integration tests verify real SQL, migrations, tenant isolation, transaction
  rollback, concurrency and framework wiring. A mocked repository cannot establish
  actual database ordering or locking behavior.

See the [default test guide](backend-business-unit-tests.md) and
[PostgreSQL integration guide](backend-integration-tests.md) for suite boundaries.
