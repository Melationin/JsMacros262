plugins {
    java
    `maven-publish`
}

val mod_version: String by project.properties

base {
    archivesName = "jsmacrosplus-api"
}

version = mod_version
group = "xyz.wagyourtail.jsmacros"

java {
    // no toolchain block: toolchain auto-detection fails on JitPack (foojay is
    // blocked there); compile with whatever JDK >= 21 the daemon runs on
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "jsmacrosplus-api"
        }
    }
}
