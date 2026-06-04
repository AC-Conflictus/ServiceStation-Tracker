plugins {
    java
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.diffplug.spotless") version "6.25.0"
}

group = "edu.austincollege"
version = "0.0.1-SNAPSHOT"

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
    // Highcharts is non-free for commercial/government use; AC IT must confirm licensing (🏫).
    implementation("org.webjars:bootstrap:5.3.3")
    implementation("org.webjars:jquery:3.7.1")
    implementation("org.webjars:datatables:2.1.8")
    implementation("org.webjars:highcharts:11.2.0")

    // Drivers: H2 for local dev/test (PostgreSQL compatibility mode), Postgres for prod.
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter:1.20.3")
    testImplementation("org.testcontainers:postgresql:1.20.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
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

// `check` runs the formatter verification (spotlessCheck) and tests.
tasks.named("check") {
    dependsOn("spotlessCheck")
}
