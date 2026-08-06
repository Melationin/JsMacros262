plugins {
    id("xyz.wagyourtail.unimined") version "1.4.2-SNAPSHOT"
}

base {
    archivesName = "jsmacrosplus-addon-template"
}

version = "1.0.0"
group = "com.example"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25

    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

unimined.minecraft {
    version("26.2")
    side("joined")

    mappings {
        mojmap()
    }

    fabric {
        loader("0.19.3")
    }
}

dependencies {
    // JsMacrosPlus API - the only JsMacrosPlus dependency an addon needs at compile time.
    // Build it first in the JsMacrosPlus repo with: ./gradlew :jsm-api:publishToMavenLocal
    implementation("xyz.wagyourtail.jsmacros:jsmacrosplus-api:2.0.0")
}

// expand ${version} in fabric.mod.json (same as the main mod's build)
tasks.processResources {
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

// ---------------------------------------------------------------------------
// TypeScript API docs (.d.ts) for this addon's @Library / @Event classes.
// Build the doclet first in the JsMacros repo with:
//     ./gradlew :doclet:publishToMavenLocal
// Then run:  ./gradlew genTSDoc
// Output:    build/typescript/headers/<name>-<version>.d.ts
// ---------------------------------------------------------------------------
val tsdoclet by configurations.creating

dependencies {
    tsdoclet("xyz.wagyourtail.jsmacros:jsmacrosplus-doclet:2.0.0")
}

tasks.register<Javadoc>("genTSDoc") {
    group = "documentation"
    description = "Generates TypeScript API declarations (.d.ts) for this addon's @Library/@Event classes"
    dependsOn(tsdoclet)

    source = sourceSets.main.get().allJava
    classpath = sourceSets.main.get().compileClasspath

    setDestinationDir(layout.buildDirectory.dir("typescript/headers").get().asFile)

    options.doclet = "xyz.wagyourtail.doclet.tsdoclet.Main"
    options.docletpath(*tsdoclet.files.toTypedArray())
    (options as CoreJavadocOptions).addStringOption("v", project.version.toString())
    // name the header after this addon instead of "JsMacros"
    (options as CoreJavadocOptions).addStringOption("name", base.archivesName.get())
    // skip globals so this header can be merged with the main mod's JsMacros-*.d.ts
    (options as CoreJavadocOptions).addBooleanOption("no-globals", true)
}
