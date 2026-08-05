plugins {
    java
    `maven-publish`
}

val mod_version: String by project.properties

base {
    archivesName = "jsmacros-api"
}

version = mod_version
group = "xyz.wagyourtail.jsmacros"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.jar {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "jsmacros-api"
        }
    }
}
