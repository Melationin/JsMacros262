plugins {
    `java`
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    // was previously provided by buildSrc's gradleApi()
    implementation("org.jetbrains:annotations:24.0.1")
}

val mod_version: String by project.properties

base {
    archivesName = "jsmacrosplus-doclet"
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

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.jar {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "jsmacrosplus-doclet"
        }
    }
}
