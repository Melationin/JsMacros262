plugins {
    id("net.fabricmc.fabric-loom") version "1.16.2"
}

val archives_base_name: String by project.properties
val mod_version: String by project.properties
val maven_group: String by project.properties

base {
    archivesName.set(archives_base_name)
}

version = mod_version
group = maven_group

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get().toInt())
    targetCompatibility = JavaVersion.toVersion(libs.versions.java.get().toInt())

    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
}

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://api.modrinth.com/maven/")
    mavenCentral()
}

sourceSets.main {
    java.setSrcDirs(listOf(
        "src/main/java",
        "src/core/java",
        "src/client/java",
        "src/fabric/java"
    ))
    resources.setSrcDirs(listOf(
        "src/main/resources",
        "src/core/resources",
        "src/client/resources",
        "src/fabric/resources"
    ))
}

loom {
    // Minecraft 26.2 is non-obfuscated, so Loom's non-remapping plugin is used.
    accessWidenerPath.set(file("src/main/resources/jsmacrosplus.accesswidener"))
    fabricModJsonPath.set(file("src/fabric/resources/fabric.mod.json"))

    runs {
        named("client") {
            runDir("run")
            vmArg("-XX:-EnableJVMCI")
        }
    }
}


val jsmacrosExtensionInclude by configurations.creating

dependencies {
    minecraft("com.mojang:minecraft:${libs.versions.minecraft.get()}")
    implementation("net.fabricmc:fabric-loader:${libs.versions.fabric.loader.get()}")

    implementation(project(":jsm-api"))
    if (!providers.gradleProperty("apiOnly").isPresent) {
        implementation("dev.jsbackend:js-backend-api:0.1.0")
    }
    include(project(":jsm-api"))

    compileOnly(libs.asm)

    val fabricApiVersion = libs.versions.fapi.get()
    val embeddedFabricModules = listOf(
        "fabric-api-base",
        "fabric-rendering-v1",
        "fabric-lifecycle-events-v1",
        "fabric-key-mapping-api-v1",
        "fabric-resource-loader-v1",
        "fabric-command-api-v2"
    )

    for (module in embeddedFabricModules) {
        implementation(fabricApi.module(module, fabricApiVersion))
        include(fabricApi.module(module, fabricApiVersion))
    }

    implementation(fabricApi.module("fabric-screen-api-v1", fabricApiVersion))
    compileOnly(libs.modmenu)
    runtimeOnly(libs.modmenu)
    compileOnly(libs.sodium)
    runtimeOnly(libs.sodium)
    implementation(libs.malilib)

    implementation(libs.prism4j)
    include(libs.prism4j)
    implementation(libs.nv.websocket)
    implementation(libs.javassist)
    implementation(libs.joor)
    include(libs.nv.websocket)
    include(libs.javassist)
    include(libs.joor)

    for (file in file("extension").listFiles() ?: emptyArray()) {
        if (!file.isDirectory || file.name in listOf("build", "src", ".gradle", "gradle")) continue

        runtimeOnly(project(":extension:${file.name}"))

        if (file.resolve("subprojects.txt").exists()) {
            for (subproject in file.resolve("subprojects.txt").readLines()) {
                runtimeOnly(project(":extension:${file.name}:$subproject"))
            }
        }
    }
}

val removeDist by tasks.registering(Delete::class) {
    delete(File(rootProject.rootDir, "dist"))
}

tasks.clean.configure {
    finalizedBy(removeDist)
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("dependencies", jsmacrosExtensionInclude.files)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }

    filesMatching("jsmacros.extension.json") {
        expand("dependencies" to jsmacrosExtensionInclude.files.map { "\"META-INF/jsmacrosdeps/${it.name}\"" }.joinToString(", "))
    }
}

tasks.jar {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(jsmacrosExtensionInclude.files) {
        include("*")
        into("META-INF/jsmacrosdeps")
    }
}

val documentedSources = files("src/main/java", "src/core/java").asFileTree.matching {
    include("**/*.java")
}

val generateTSDoc by tasks.registering(Javadoc::class) {
    group = "documentation"
    description = "Generates the typescript documentation for the project"
    dependsOn(":doclet:jar")

    source = documentedSources
    classpath = sourceSets.main.get().compileClasspath
    setDestinationDir(File(rootProject.layout.buildDirectory.get().asFile, "docs/typescript/headers/"))
    options.doclet = "xyz.wagyourtail.doclet.tsdoclet.Main"
    options.docletpath(project(":doclet").tasks.jar.get().archiveFile.get().asFile)
    (options as CoreJavadocOptions).addStringOption("v", mod_version)
}

val copyTSDoc by tasks.registering(Copy::class) {
    group = "documentation"
    description = "Copies the typescript files to the build folder"
    dependsOn(generateTSDoc)

    from(File(rootProject.rootDir, "docs/typescript"))
    into(File(rootProject.layout.buildDirectory.get().asFile, "docs/typescript"))
}

val generateWebDoc by tasks.registering(Javadoc::class) {
    group = "documentation"
    description = "Generates the web documentation for the project"
    dependsOn(":doclet:jar")

    source = documentedSources
    classpath = sourceSets.main.get().compileClasspath
    setDestinationDir(File(rootProject.layout.buildDirectory.get().asFile, "docs/web/"))
    options.doclet = "xyz.wagyourtail.doclet.webdoclet.Main"
    options.docletpath(project(":doclet").tasks.jar.get().archiveFile.get().asFile)
    (options as CoreJavadocOptions).addStringOption("v", mod_version)
    (options as CoreJavadocOptions).addStringOption("mcv", libs.versions.minecraft.get())
    (options as StandardJavadocDocletOptions).links("https://docs.oracle.com/javase/8/docs/api/", "https://www.javadoc.io/doc/org.slf4j/slf4j-api/1.7.30/", "https://javadoc.io/doc/com.neovisionaries/nv-websocket-client/latest/")
}

val copyWebDoc by tasks.registering(Copy::class) {
    group = "documentation"
    description = "Copies the web documentation to the build folder"
    dependsOn(generateWebDoc)

    from(File(rootProject.rootDir, "docs/web"))
    into(File(rootProject.layout.buildDirectory.get().asFile, "docs/web"))

    inputs.property("version", project.version)

    filesMatching("index.html") {
        expand("version" to project.version)
    }
}

val createDist by tasks.registering(Copy::class) {
    group = "build"
    description = "Creates all files for the distribution of the project"
    dependsOn(copyTSDoc, copyWebDoc)

    from(File(rootProject.layout.buildDirectory.get().asFile, "docs"))
    from(File(rootProject.layout.buildDirectory.get().asFile, "libs"))
    into(File(rootProject.rootDir, "dist"))
}

tasks.build.configure {
    finalizedBy(createDist)
}
