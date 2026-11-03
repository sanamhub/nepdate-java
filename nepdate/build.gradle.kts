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
            // The exhaustive walk and the jqwik properties would repeat the whole search for
            // every mutant.
            "io.github.sanamhub.nepdate.java.ExhaustiveTest",
            "io.github.sanamhub.nepdate.java.PropertiesTest",
            // These five read a vector row per dynamic invocation: 1018 to 10,180 of them. PIT
            // selects every covering invocation for each mutant, so keeping them turns one unit
            // into tens of thousands of test runs. They run in the normal gate on every push; the
            // remaining tests still cover the lines they reach (ADR-0010).
            "io.github.sanamhub.nepdate.java.DatesVectorTest",
            "io.github.sanamhub.nepdate.java.AddDaysVectorTest",
            "io.github.sanamhub.nepdate.java.AddMonthsVectorTest",
            "io.github.sanamhub.nepdate.java.MonthBoundariesVectorTest",
            "io.github.sanamhub.nepdate.java.FormatPatternVectorTest",
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
    // One small C1-only minion: the gate must also run on a developer machine that is short of
    // commit memory, and the mutation score does not depend on the JIT tier. On the scheduled
    // runner the same settings finish in a few minutes.
    jvmArgs.set(
        listOf(
            "-Xmx256m",
            "-XX:MaxMetaspaceSize=128m",
            "-XX:ReservedCodeCacheSize=64m",
            "-XX:TieredStopAtLevel=1",
            "-XX:+UseSerialGC",
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
