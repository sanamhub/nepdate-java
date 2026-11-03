pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Provisions the JDK toolchain (ADR-0001) on a machine that does not have it already.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "nepdate-java"

include(":nepdate", ":nepdate-patro", ":codegen", ":benchmarks")

// The project names differ from the Kotlin port's published artifact names on purpose: Gradle
// substitutes a dependency whose group:name matches a project in the build, and the benchmark
// module must resolve io.github.sanamhub:nepdate from Maven Central, not this build's core.
project(":nepdate").name = "nepdate-java"
project(":nepdate-patro").name = "nepdate-java-patro"
