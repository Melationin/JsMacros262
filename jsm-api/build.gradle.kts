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

// force :doclet to be configured before this project, so its sourceSets are
// available when the jar task below is configured
evaluationDependsOn(":doclet")

tasks.jar {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    // Bundle the TS/Py/Web doclet classes into the api artifact, so addons can
    // run genTSDoc using only the api dependency (JitPack publishes a single
    // root-module artifact, there is no separate doclet artifact).
    from(project(":doclet").sourceSets.main.get().output)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "jsmacrosplus-api"
        }
    }
}
