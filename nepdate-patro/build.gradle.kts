plugins {
    alias(libs.plugins.maven.publish)
}

description = "Patro (Nepali almanac: tithi, public holidays, events) for nepdate-java."

dependencies {
    api(project(":nepdate-java"))
}

mavenPublishing {
    coordinates("io.github.sanamhub", "nepdate-java-patro", version.toString())
    pom {
        name.set("nepdate-java-patro")
        description.set("Patro for nepdate-java: tithi, public holidays and events, BS 2077 to 2084.")
    }
}
