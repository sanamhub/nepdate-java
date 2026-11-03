// A throwaway project that consumes the two published coordinates the way a user does. CI
// publishes to mavenLocal first (or the release has published to Maven Central) and runs this.
plugins {
    application
}

val nepdateVersion: String = providers.gradleProperty("nepdateVersion").getOrElse("1.0.0-rc.1")

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.github.sanamhub:nepdate-java:$nepdateVersion")
    implementation("io.github.sanamhub:nepdate-java-patro:$nepdateVersion")
}

application {
    mainClass.set("consumer.ConsumerCheck")
}

tasks.named<JavaExec>("run") {
    // The sample needs nothing but the two jars.
}
