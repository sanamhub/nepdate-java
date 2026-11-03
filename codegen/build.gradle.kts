plugins {
    application
}

description = "Generates the calendar and patro tables from shared/data, with a --check mode."

application {
    mainClass.set("io.github.sanamhub.nepdate.java.codegen.Main")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("nepdate.repoRoot", rootProject.layout.projectDirectory.asFile.absolutePath)
}

// The generator reads shared/ and the two module source trees, so it always runs from the root.
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

tasks.register<JavaExec>("generate") {
    description = "Writes the generated tables (shorthand for :codegen:run)."
    group = "build"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.github.sanamhub.nepdate.java.codegen.Main")
    workingDir = rootProject.projectDir
}

tasks.register<JavaExec>("checkTables") {
    description = "Fails when a generated table differs from shared/data."
    group = "verification"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.github.sanamhub.nepdate.java.codegen.Main")
    workingDir = rootProject.projectDir
    args("--check")
}
