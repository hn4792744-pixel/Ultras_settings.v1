plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.5"
}

group = "me.uc_hussein"
version = "v1"
description = "Ultras_settings — premium Settings GUI frontend for Ultras_TPA"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // See README "Compatibility": built against the modern Paper API only (no NMS), so bumping this
    // single line is the intended way to track newer Paper/Minecraft releases.
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
    processResources {
        filteringCharset = "UTF-8"
    }
    shadowJar {
        archiveBaseName.set("Ultras_settings")
        archiveVersion.set("v1")
        archiveClassifier.set("")
    }
    build {
        dependsOn(shadowJar)
    }
}
