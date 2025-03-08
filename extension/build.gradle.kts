plugins {
    java
    alias(libs.plugins.shadow)
}

val archives_base_name: String by project.properties
val java_version: String by project.properties

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net/")
}

dependencies {
    implementation(rootProject.sourceSets["core"].output)
    for (dependency in rootProject.configurations["minecraftLibraries"].dependencies) {
        implementation(dependency)
    }

    for (dependency in rootProject.configurations.implementation.get().dependencies) {
        implementation(dependency)
    }

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.jetbrains:annotations:20.1.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

subprojects {
    apply(plugin= "java")
    apply(plugin= "com.github.johnrengelman.shadow")

    base {
        archivesName = archives_base_name + "-${project.name}-extension"
    }

    java {
        sourceCompatibility = JavaVersion.values()[java_version.toInt() - 1]
        targetCompatibility = JavaVersion.values()[java_version.toInt() - 1]

        toolchain {
            languageVersion = JavaLanguageVersion.of(java_version.toInt())
        }
    }

    tasks.withType(JavaCompile::class).configureEach {
        options.encoding = "UTF-8"

        var javaVersion = java_version.toInt()
        if (JavaVersion.current().isJava9Compatible()) {
            options.release.set(javaVersion)
        }
    }

    val jsmacrosExtensionInclude by configurations.creating

    configurations.implementation.configure {
        extendsFrom(jsmacrosExtensionInclude)
    }

    repositories {
        mavenCentral()
        maven("https://libraries.minecraft.net/")
    }

    dependencies {

        implementation(parent!!.sourceSets.main.get().output)
        for (dependency in parent!!.configurations.implementation.get().dependencies) {
            implementation(dependency)
        }

        testImplementation(parent!!.sourceSets.test.get().output)
    }

    afterEvaluate {
        var includeFiles = files(jsmacrosExtensionInclude) - files(parent!!.configurations.findByName("jsmacrosExtensionInclude") ?: emptySet<File>()).filter{ it.name.endsWith(".jar") }

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

}