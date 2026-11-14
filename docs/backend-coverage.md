# Backend code coverage

The Gradle JaCoCo plugin records coverage for both backend test suites. It produces
HTML reports for browsing and XML reports for CI tools. JaCoCo is pinned to 0.8.14,
which supports the project's Java 21 bytecode. See the official
[Gradle JaCoCo guide](https://docs.gradle.org/current/userguide/jacoco_plugin.html)
and [JaCoCo release notes](https://www.jacoco.org/jacoco/trunk/doc/changes.html).

## Run and open reports

From `backend/`:

```sh
# Default suite: unit tests plus lightweight HTTP/filesystem/architecture tests.
# Does not require Docker. Coverage HTML/XML are generated automatically.
./gradlew test
open build/reports/jacoco/test/html/index.html

# PostgreSQL integration coverage. Requires Docker to be running.
./gradlew integrationTest
open build/reports/jacoco/integrationTest/html/index.html

# Both suites and their combined coverage. Requires Docker.
./gradlew jacocoCombinedReport
open build/reports/jacoco/combined/html/index.html
```

`open` is the macOS command. On other platforms, open the HTML file in a browser.
No application container or development database needs to be started for these
tasks. Integration tests create their own temporary PostgreSQL containers.

You can also request reports directly. Each report task depends on the appropriate
test task, so a clean checkout does not require a separate test command first:

```sh
./gradlew jacocoTestReport
./gradlew jacocoIntegrationTestReport
```

## Outputs

| Suite | HTML | XML |
| --- | --- | --- |
| Default `test` | `build/reports/jacoco/test/html/index.html` | `build/reports/jacoco/test/jacoco.xml` |
| `integrationTest` | `build/reports/jacoco/integrationTest/html/index.html` | `build/reports/jacoco/integrationTest/jacoco.xml` |
| Combined | `build/reports/jacoco/combined/html/index.html` | `build/reports/jacoco/combined/jacoco.xml` |

Execution data is stored separately in `build/jacoco/test.exec` and
`build/jacoco/integrationTest.exec`. The combined report merges those observations;
it does not add or average the individual coverage percentages.

## Interpreting coverage

- Reports measure production code in the `main` source set. No project-specific
  exclusions hide controllers, entities, mappers or other application classes.
  JaCoCo's own generated-bytecode filters still apply.
- Compare line coverage and branch coverage. A covered line does not prove every
  decision on that line was tested.
- The default-suite report is not strictly pure unit coverage: `test` also includes
  existing lightweight integration and architecture checks.
- Filtered commands such as `./gradlew test --tests '*TenantFilterTest'` produce
  coverage for that selection only. Run the unfiltered suite before treating its
  report as complete. Use `--rerun-tasks` when a fresh run is needed.
- Test finalizers attempt to generate per-suite reports even when a test fails,
  provided execution data was written. Such reports can be partial. Compilation
  failures or JVM startup failures may prevent report generation; check the task
  result and report timestamp instead of trusting an older HTML file.
- Coverage reporting does not enforce a minimum percentage or introduce a new
  build gate. Establish a measured baseline before choosing thresholds.
- Unit coverage tasks never depend on the integration suite. The standard `check`
  and `build` lifecycle still does not implicitly run `integrationTest`.

## Verification status

The 81 additional tests listed in [the business test guide](backend-business-unit-tests.md)
were added after the measured run below. They have not been compiled or run.
The percentages below remain the previous baseline, and the 80% line/branch
target has not been verified. Regenerate the combined report before assessing
the effect of the new tests.

A subsequent local combined XML was inspected at 61.56% line and 39.76% branch
coverage. The requested target is now 100%; it has not been achieved or verified.
`SearchUtilsTest` was then added to exercise the public search/filter API's scalar
conversion, comparison, wildcard escaping, collection membership, nested grouping,
approved sorting, invalid configuration and collection joins. This additional
suite has not been compiled or run. No production class exclusions were added.

Verified on 2026-11-13 with `./gradlew jacocoCombinedReport`: 140 tests in the
default suite and 31 PostgreSQL integration tests passed, with zero failures,
errors or skips. The command completed successfully and all three HTML/XML
reports were generated. `git diff --check` also passed.

| Report | Line coverage | Branch coverage |
| --- | ---: | ---: |
| Default suite | 33.05% | 20.65% |
| Integration suite | 28.04% | 19.05% |
| Combined | 48.02% | 32.33% |

These values are the measured baseline from that run, not a minimum requirement
or a promise that every application behavior is covered. Regenerate reports
after code or test changes.
