plugins {
    application
}

description = "JMH benchmarks for nepdate-java, with the published Kotlin artifact as the comparison."

dependencies {
    implementation(project(":nepdate-java"))
    implementation(project(":nepdate-java-patro"))
    // The published Kotlin port, called from Java in the same JVM and the same run (ADR-0008).
    implementation(libs.kotlin.nepdate)
    implementation(libs.kotlin.nepdate.patro)
    // The one Maven Central competitor that passes BENCHMARK section 7 (ADR-0008). Its POM pulls
    // the Compose runtime that the date picker widgets use; the conversion classes this harness
    // calls do not need it, and it is not on Maven Central, so the Compose modules are excluded.
    implementation(libs.shivathapaa.core) {
        exclude(group = "androidx.compose.runtime")
        exclude(group = "org.jetbrains.compose.runtime")
        exclude(group = "org.jetbrains.compose.ui")
        exclude(group = "org.jetbrains.compose.foundation")
    }
    implementation(libs.jmh.core)
    annotationProcessor(libs.jmh.generator)
}

application {
    mainClass.set("org.openjdk.jmh.Main")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

tasks.register<JavaExec>("jmh") {
    description = "Runs the benchmarks; pass JMH arguments with -PjmhArgs=..."
    group = "verification"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.openjdk.jmh.Main")
    workingDir = rootProject.projectDir
    jvmArgs(
        "-Djmh.blackhole.forced=true",
    )
    providers.gradleProperty("jmhArgs").orNull?.split(" ")?.forEach { args(it) }
}
tasks.register("printClasspath") {
    // The benchmark is run directly with java so the process can be pinned to one core; this file
    // is the classpath that command uses, with both project jars built first.
    dependsOn(":nepdate-java:jar", ":nepdate-java-patro:jar", "classes")
    val classpath = sourceSets.main.get().runtimeClasspath
    val output = layout.buildDirectory.file("benchmark-classpath.txt")
    doLast {
        val text = classpath.asPath
        output.get().asFile.writeText(text)
        println(text)
    }
}
