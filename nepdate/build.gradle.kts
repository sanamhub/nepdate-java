plugins {
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.pitest)
}

description = "Bikram Sambat (Nepali calendar) dates for Java."

dependencies {
    // The core has no runtime dependency: JSpecify is compile-only, and java.base plus java.time
    // cover everything else.
}

// PIT mutates the core only (ADR-0010). The generated table is data, not code under test; the
// exhaustive walk and the jqwik properties would repeat for every mutant, so the deterministic
// tests are the ones PIT exercises. The two versions are pinned here because the plugin extension
// shadows the version catalog inside its block; Dependabot bumps this file too.
pitest {
    pitestVersion.set("1.30.1")
    junit5PluginVersion.set("1.2.3")
    targetClasses.set(setOf("io.github.sanamhub.nepdate.java.*"))
    targetTests.set(setOf("io.github.sanamhub.nepdate.java.*"))
    excludedClasses.set(setOf("io.github.sanamhub.nepdate.java.CalendarTable"))
    excludedTestClasses.set(
        setOf(
            "io.github.sanamhub.nepdate.java.ExhaustiveTest",
            "io.github.sanamhub.nepdate.java.PropertiesTest",
            "io.github.sanamhub.nepdate.java.DocsExamplesTest",
        ),
    )
    threads.set(1)
    outputFormats.set(setOf("HTML", "XML"))
    timestampedReports.set(false)
    // The thresholds are in gradle.properties so a local run and CI use the same numbers.
    mutationThreshold.set(
        providers.gradleProperty("nepdateMutationThreshold").orNull?.toInt() ?: 0,
    )
    maxSurviving.set(
        providers.gradleProperty("nepdateMaxSurviving").orNull?.toInt() ?: 0,
    )
    // PIT starts its own JVMs, so the vector tests need the shared directory there too, and a
    // small explicit heap keeps four minions from competing with the build's own JVM.
    jvmArgs.set(
        listOf(
            "-Xmx512m",
            "-XX:MaxMetaspaceSize=256m",
            "-Dnepdate.shared=${rootProject.file("shared").absolutePath}",
            "-Dnepdate.repoRoot=${rootProject.projectDir.parentFile.absolutePath}",
        ),
    )
}

mavenPublishing {
    coordinates("io.github.sanamhub", "nepdate-java", version.toString())
    pom {
        name.set("nepdate-java")
        description.set("Bikram Sambat (Nepali calendar) dates for Java: conversion, arithmetic, formatting, parsing, ranges and fiscal years.")
    }
}
