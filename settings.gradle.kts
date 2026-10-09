pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // One shared source tree built for every Minecraft version (versioned comments: //? if >=1.21.9).
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Picks fabric-loom-remap for obfuscated versions (< 26.1) and fabric-loom for 26.1+.
    id("dev.kikugie.loom-back-compat") version "0.4.2"
}

stonecutter {
    create(rootProject) {
        // Every node has versions/<mc>/gradle.properties with its dependency versions.
        versions("1.21.11")
        vcsVersion = "1.21.11"
    }
}

rootProject.name = "shard-client"
