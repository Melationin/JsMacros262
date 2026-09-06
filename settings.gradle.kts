pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

val jsBackendDir = providers.gradleProperty("jsBackendDir").orNull
    ?.let { file(it) }
    ?: file("../js-backend")

if (jsBackendDir.resolve("settings.gradle.kts").isFile) {
    includeBuild(jsBackendDir) {
        dependencySubstitution {
            substitute(module("dev.jsbackend:js-backend-api"))
                .using(project(":api"))
        }
    }
} else if (!providers.gradleProperty("apiOnly").isPresent) {
    throw GradleException(
        "JS-backend was not found at $jsBackendDir. " +
            "Clone https://github.com/Melationin/JS-backend.git there or set -PjsBackendDir=<path>."
    )
}

include("site")
include("jsm-api")
include("doclet")

include("extension")
for (file in file("extension").listFiles() ?: emptyArray()) {
    if (!file.isDirectory || file.name in listOf("build", "src", ".gradle", "gradle")) continue
    include("extension:${file.name}")

    if (file.resolve("subprojects.txt").exists()) {
        for (subproject in file.resolve("subprojects.txt").readLines()) {
            include("extension:${file.name}:$subproject")
        }
    }
}


dependencyResolutionManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.terraformersmc.com/releases/")
        maven("https://api.modrinth.com/maven/")
        mavenCentral()
    }

    versionCatalogs {
        for (file in file("extension").listFiles() ?: emptyArray()) {
            if (!file.isDirectory || file.name in listOf("build", "src", ".gradle", "gradle")) continue
            val extensionName = file.name
            val libPath = "extension/$extensionName/gradle/$extensionName.versions.toml"

            if (file(libPath).exists()) {
                create("${extensionName}Libs") {
                    from(files(libPath))
                }
            }
        }
    }
}


rootProject.name = "jsmacrosplus"
