// The two published libraries, the table generator and the JMH benchmarks, in one Gradle build.
// The layout and the gates are ADR-0001 and ADR-0004; the release setup is ADR-0007.

import com.diffplug.gradle.spotless.SpotlessExtension
import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsExtension
import com.vanniktech.maven.publish.DeploymentValidation
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.spotbugs) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.maven.publish) apply false
}

// Java 17 is the floor (ADR-0001). CI runs the whole build on 17, 21 and 25 through
// -PnepdateToolchain; the bytecode target stays 17 either way.
val javaVersion: Int = providers.gradleProperty("nepdateToolchain").orNull?.toInt() ?: 17
val coverageMinimum: String = providers.gradleProperty("nepdateCoverageMinimum").get()
val googleJavaFormatVersion: String = libs.versions.googleJavaFormat.get()
val jacocoVersion: String = libs.versions.jacoco.get()
val spotbugsVersion: String = libs.versions.spotbugsVersion.get()
// The catalog is only an extension of the root project; subprojects need this captured copy.
val catalog = libs

// The generated tables are data, not code under test: :codegen --check and the count tests in
// :nepdate-patro cover them, and they would only dilute a line-coverage gate.
val generatedClasses = listOf(
    "io/github/sanamhub/nepdate/java/CalendarTable.class",
    "io/github/sanamhub/nepdate/java/patro/PatroTable.class",
)

val libraryProjects = setOf(":nepdate-java", ":nepdate-java-patro")

fun coverageTree(root: File): FileTree = rootProject.fileTree(root) { exclude(generatedClasses) }

subprojects {
    pluginManager.withPlugin("java") {
        apply(plugin = "com.diffplug.spotless")
        configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
        }
        tasks.withType<JavaCompile>().configureEach {
            options.release.set(17)
            options.encoding = "UTF-8"
            options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
        }
    }

    pluginManager.withPlugin("com.diffplug.spotless") {
        configure<SpotlessExtension> {
            java {
                target("**/*.java")
                // Written by :codegen and compared byte for byte by its --check mode.
                targetExclude("**/CalendarTable.java", "**/PatroTable.java")
                googleJavaFormat(googleJavaFormatVersion)
                removeUnusedImports()
                formatAnnotations()
            }
        }
    }
}

configure(subprojects.filter { it.path in libraryProjects }) {
    apply(plugin = "java-library")
    apply(plugin = "jacoco")
    apply(plugin = "com.github.spotbugs")
    configure<SpotBugsExtension> {
        toolVersion.set(spotbugsVersion)
        effort.set(Effort.MAX)
        // Every bug pattern at every priority: a finding is fixed, not filtered (ADR-0004 gate 2).
        reportLevel.set(Confidence.LOW)
        excludeFilter.set(rootProject.layout.projectDirectory.file("config/spotbugs/exclude.xml"))
    }
    // Only the shipped classes are analysed; the test fixtures are not shipped and their
    // assertions would add findings, not safety (ADR-0004).
    tasks.matching { it.name == "spotbugsTest" }.configureEach { enabled = false }
    dependencies {
        "compileOnly"(catalog.jspecify)
        "testImplementation"(platform(catalog.junit.bom))
        "testImplementation"(catalog.junit.jupiter)
        "testImplementation"(catalog.jqwik)
        "testRuntimeOnly"(catalog.junit.platform.launcher)
    }
    // The publishing plugin adds the sources and javadoc jars; adding them here as well creates a
    // second javadoc jar task and a task-ordering conflict in Gradle 9.
    configure<JavaPluginExtension> {
        withSourcesJar()
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        systemProperty("nepdate.shared", rootProject.layout.projectDirectory.dir("shared").asFile.absolutePath)
        systemProperty("nepdate.repoRoot", rootProject.layout.projectDirectory.asFile.absolutePath)
        maxHeapSize = "768m"
        // The README and API samples are Java code DocsExamplesTest compiles and runs, so a doc
        // change re-runs the test.
        inputs.file(rootProject.layout.projectDirectory.file("README.md"))
        inputs.file(rootProject.layout.projectDirectory.file("docs/API.md"))
        testLogging.events("failed")
        testLogging.exceptionFormat = TestExceptionFormat.FULL
    }
    tasks.withType<Javadoc>().configureEach {
        val standard = options as StandardJavadocDocletOptions
        standard.encoding = "UTF-8"
        standard.charSet = "UTF-8"
        standard.docEncoding = "UTF-8"
        // Every public type and member is documented (ADR-0004 gate 5): a missing comment fails
        // the javadoc jar that Maven Central serves.
        standard.addBooleanOption("Xdoclint:all", true)
        standard.addBooleanOption("Werror", true)
    }
    configure<JacocoPluginExtension> {
        toolVersion = jacocoVersion
    }
    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
        classDirectories.setFrom(files(classDirectories.files.map { coverageTree(it) }))
    }
    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        dependsOn(tasks.named("test"))
        classDirectories.setFrom(files(classDirectories.files.map { coverageTree(it) }))
        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = coverageMinimum.toBigDecimal()
                }
            }
        }
    }
    tasks.named("check") {
        dependsOn(tasks.named("jacocoTestCoverageVerification"))
    }
    pluginManager.withPlugin("com.vanniktech.maven.publish") {
        configure<MavenPublishBaseExtension> {
            // The same two calls as the Kotlin port: without them no Central task is registered
            // and nothing is signed. VALIDATED waits for the Portal's check, so a rejected upload
            // fails the job instead of passing silently.
            publishToMavenCentral(automaticRelease = true, validateDeployment = DeploymentValidation.VALIDATED)
            // CI's publishToMavenLocal runs without the key; release.yml checks every .asc before
            // anything is uploaded, so a missing key cannot ship unsigned artifacts.
            if (providers.gradleProperty("signingInMemoryKey").isPresent) {
                signAllPublications()
            }
            pom {
                inceptionYear.set("2026")
                url.set("https://github.com/sanamhub/nepdate-java")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("sanamhub")
                        name.set("Sanam Pakuwal")
                        url.set("https://github.com/sanamhub")
                    }
                }
                scm {
                    url.set("https://github.com/sanamhub/nepdate-java")
                    connection.set("scm:git:https://github.com/sanamhub/nepdate-java.git")
                    developerConnection.set("scm:git:ssh://git@github.com/sanamhub/nepdate-java.git")
                }
            }
        }
    }
}

// ADR-0004: the core artifact carries no patro data and never depends on the patro. The marker is
// an English event name longer than 15 bytes, so no compiler can store it inline in code.
val checkCoreSplit = tasks.register("checkCoreSplit") {
    description = "Fails if the core jar or POM names the patro, or the core holds patro data."
    group = "verification"
    val coreJar = project(":nepdate-java").tasks.named<Jar>("jar").flatMap { it.archiveFile }
    val patroJar = project(":nepdate-java-patro").tasks.named<Jar>("jar").flatMap { it.archiveFile }
    dependsOn(":nepdate-java:jar", ":nepdate-java-patro:jar")
    inputs.files(coreJar, patroJar)
    doLast {
        val marker = "World Day Against Human Trafficking".toByteArray(Charsets.UTF_8)

        fun ByteArray.has(needle: ByteArray): Boolean =
            needle.isEmpty() || (0..size - needle.size).any { start -> needle.indices.all { this[start + it] == needle[it] } }

        fun entries(jar: File): Map<String, ByteArray> =
            java.util.zip.ZipFile(jar).use { zip ->
                zip.entries().toList().filter { !it.isDirectory }.associate { it.name to zip.getInputStream(it).readBytes() }
            }

        val problems = mutableListOf<String>()
        val core = entries(coreJar.get().asFile)
        core.filterValues { it.has(marker) }.keys.forEach { problems += "core jar entry $it holds patro data" }
        if (entries(patroJar.get().asFile).values.none { it.has(marker) }) {
            problems += "the patro jar lacks the patro data"
        }
        core.filterKeys { it.endsWith(".class") }.forEach { (name, bytes) ->
            if (bytes.has("io/github/sanamhub/nepdate/java/patro".toByteArray())) {
                problems += "$name in the core refers to the patro package"
            }
        }
        if (problems.isNotEmpty()) throw GradleException("checkCoreSplit:\n" + problems.joinToString("\n"))
        logger.lifecycle("checkCoreSplit: the core is free of patro data and references")
    }
}

tasks.register("checkAll") {
    description = "The Gradle gates scripts/ci.sh runs, for an IDE or a one-command local check."
    group = "verification"
    dependsOn(
        ":nepdate-java:check",
        ":nepdate-java-patro:check",
        ":nepdate-java:javadoc",
        ":nepdate-java-patro:javadoc",
        ":codegen:check",
        ":benchmarks:classes",
        checkCoreSplit,
    )
}


