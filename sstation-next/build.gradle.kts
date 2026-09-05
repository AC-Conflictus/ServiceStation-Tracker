plugins {
    java
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.diffplug.spotless") version "6.25.0"
}

group = "edu.austincollege"
version = "0.0.1-SNAPSHOT"

// TC-118: pin the whole Testcontainers family from one place.
//
// Spring Boot 3.3.5's BOM manages `testcontainers-bom` at 1.19.8, which wins over an explicit
// version on a *transitive* artifact. Declaring `junit-jupiter:1.20.3` therefore produced a
// mixed-version classpath -- `testcontainers:1.20.3 -> 1.19.8` -- because core arrived
// transitively and got pulled back. Docker strategy detection lives in core, so bumping the
// explicit coordinates alone changed nothing at all; that is why an earlier attempt to fix the
// silent skip by moving to 1.21.3 appeared to have no effect. Override the BOM property instead
// and the whole family moves together.
extra["testcontainers.version"] = "1.21.3"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Web + view layer
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")

    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Mail — approve/reject notifications (TC-021 / TC-108a). JavaMailSender is only
    // auto-configured when spring.mail.host is set (prod), so dev stays a no-op.
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Persistence
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Ops
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // CSV export on the report pages (TC-023 / TC-108c). OpenCSV handles RFC-4180 escaping so
    // org/event names containing commas or quotes can't corrupt the output.
    implementation("com.opencsv:opencsv:5.9")

    // PDF export of the per-student report (TC-024 / TC-108d). openhtmltopdf renders a Thymeleaf
    // XHTML template; jsoup cleans the rendered HTML into the well-formed DOM it requires.
    implementation("com.openhtmltopdf:openhtmltopdf-pdfbox:1.0.10")
    implementation("org.jsoup:jsoup:1.17.2")

    // Vendored frontend assets (TC-107) — served from the app jar at /webjars/**, no CDN.
    // Charting is Chart.js (MIT). It replaced Highcharts in TC-119: Highcharts is not free for
    // commercial/institutional use, and vendoring it here meant we were redistributing it.
    implementation("org.webjars:bootstrap:5.3.3")
    implementation("org.webjars:jquery:3.7.1")
    implementation("org.webjars:datatables:2.1.8")
    implementation("org.webjars:chartjs:4.4.3")

    // Drivers: H2 for local dev/test (PostgreSQL compatibility mode), Postgres for prod.
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()

    // TC-118: pin the Docker Engine API version docker-java negotiates.
    //
    // Testcontainers' shaded docker-java defaults to API v1.32. Docker Engine 29 raised its
    // MinAPIVersion to 1.40, so `/v1.32/info` comes back HTTP 400 during strategy detection,
    // every strategy fails, and `@Testcontainers(disabledWithoutDocker = true)` quietly marks
    // DemoProfileIntegrationTest SKIPPED while the build still reports BUILD SUCCESSFUL.
    //
    // 1.41 is the widest-compatibility choice: it is >= Docker 29's floor of 1.40 and still well
    // under the ceiling of Docker 20.10 (1.41), so this works on both old and new daemons. Raise
    // it only if a container feature needs a newer API than 1.41 provides.
    systemProperty("api.version", "1.41")
}

// Local `./gradlew bootRun` boots the dev profile so the seeded admin/student/moderator
// accounts (DevDataSeeder) exist. Prod runs with -Dspring.profiles.active=prod (no seeding).
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    systemProperty("spring.profiles.active", "dev")
}

spotless {
    java {
        googleJavaFormat("1.23.0")
        target("src/**/*.java")
        importOrder()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// TC-118: a skipped test must never pass for a green build.
//
// `DemoProfileIntegrationTest` is the only test that touches real PostgreSQL, and it carries
// `@Testcontainers(disabledWithoutDocker = true)` -- so when Docker is unreachable it reports
// SKIPPED and the build still succeeds. That is the exact trap
// TC-118 was raised for: `./gradlew check` went green having proven nothing about the database we
// actually deploy to.
//
// The rule here is deliberately blunt and needs no allowlist to maintain: if *any* test in the
// suite was skipped rather than executed, fail and name it. The suite runs 119/119 with zero
// skips on both a dev box and the CI runner, so a skip is always a signal, never noise.
val verifyNoSkippedTests =
    tasks.register("verifyNoSkippedTests") {
        group = "verification"
        description = "TC-118: fail the build if any test was skipped instead of executed."
        dependsOn(tasks.named("test"))

        val resultsDir = layout.buildDirectory.dir("test-results/test")
        outputs.upToDateWhen { false }

        doLast {
            val dir = resultsDir.get().asFile
            val reports =
                dir.listFiles { f: java.io.File -> f.name.startsWith("TEST-") && f.name.endsWith(".xml") }
                    ?.sortedBy { it.name }
                    .orEmpty()

            if (reports.isEmpty()) {
                throw GradleException("TC-118: no test-result XML under $dir — the test task ran nothing.")
            }

            val builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val skipped =
                reports.mapNotNull { file ->
                    val root = builder.parse(file).documentElement
                    val count = root.getAttribute("skipped").toIntOrNull() ?: 0
                    if (count > 0) "  - ${root.getAttribute("name")} ($count skipped)" else null
                }

            if (skipped.isNotEmpty()) {
                throw GradleException(
                    buildString {
                        appendLine("TC-118: ${skipped.size} test class(es) were SKIPPED, not executed:")
                        skipped.forEach { appendLine(it) }
                        appendLine()
                        appendLine("A skipped test proves nothing, so this fails the build instead of")
                        appendLine("reporting success. If these are the Postgres-backed integration tests,")
                        appendLine("Testcontainers could not reach a Docker daemon. Check that Docker is")
                        appendLine("running, and see the `api.version` system property set on the test task")
                        appendLine("above — Docker Engine 29 rejects the API version docker-java defaults to.")
                    }
                )
            }
        }
    }

// `check` runs the formatter verification (spotlessCheck), the tests, and the no-silent-skip gate.
tasks.named("check") {
    dependsOn("spotlessCheck", verifyNoSkippedTests)
}
