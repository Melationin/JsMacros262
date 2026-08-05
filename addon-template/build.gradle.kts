plugins {
    id("xyz.wagyourtail.unimined") version "1.4.2-SNAPSHOT"
}

base {
    archivesName = "jsmacros-addon-template"
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
    // JsMacros API - the only JsMacros dependency an addon needs at compile time.
    // Build it first in the JsMacros repo with: ./gradlew :jsm-api:publishToMavenLocal
    implementation("xyz.wagyourtail.jsmacros:jsmacros-api:2.0.0")
}
