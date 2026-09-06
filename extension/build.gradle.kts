plugins {
    java
    `java-test-fixtures`
    alias(libs.plugins.shadow) apply false
}

val archives_base_name: String by project.properties

dependencies {
    testFixturesApi(rootProject.sourceSets.main.get().output)
    testFixturesApi(project(":jsm-api"))
    testFixturesApi(libs.junit.api)
    testFixturesApi(libs.slf4j)
}

java {
    sourceCompatibility = JavaVersion.toVersion(rootProject.libs.versions.java.get().toInt())
    targetCompatibility = JavaVersion.toVersion(rootProject.libs.versions.java.get().toInt())

    toolchain {
        languageVersion = JavaLanguageVersion.of(rootProject.libs.versions.java.get().toInt())
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.gradleup.shadow")

    base {
        archivesName = archives_base_name + "-${project.name}-extension"
    }

    java {
        sourceCompatibility = JavaVersion.toVersion(rootProject.libs.versions.java.get().toInt())
        targetCompatibility = JavaVersion.toVersion(rootProject.libs.versions.java.get().toInt())

        toolchain {
            languageVersion = JavaLanguageVersion.of(rootProject.libs.versions.java.get().toInt())
        }
    }

    val jsmacrosExtensionInclude by configurations.creating

    configurations.implementation.configure {
        extendsFrom(jsmacrosExtensionInclude)
    }

    dependencies {
        implementation(rootProject.sourceSets.main.get().output)
        for (dependency in rootProject.configurations.implementation.get().dependencies) {
            implementation(dependency)
        }

        testImplementation(testFixtures(project(":extension")))
    }

    // run tests interpreted on plain-JDK-like JVMs: -XX:-EnableJVMCI makes
    // LibGraal (graal-sdk) fall back gracefully instead of crashing on
    // JVMCI-version mismatch (graal 24.0.1 vs GraalVM JDK 25)
    tasks.test {
        useJUnitPlatform()
        jvmArgs("-XX:-EnableJVMCI")
    }

    val includeFiles = files(jsmacrosExtensionInclude) -
        files(parent!!.configurations.findByName("jsmacrosExtensionInclude") ?: emptySet<File>())
            .filter { it.name.endsWith(".jar") }

    tasks.jar {
        from(includeFiles) {
            include("*")
            into("META-INF/jsmacrosdeps")
        }

        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    tasks.processResources {
        filesMatching("jsmacros.ext.*.json") {
            expand("dependencies" to includeFiles.joinToString("\", \"") { "META-INF/jsmacrosdeps/${it.name}" })
        }
    }
}
