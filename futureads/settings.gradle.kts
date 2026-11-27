pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.meteordev.org/releases") { name = "Meteor" }
        maven("https://maven.meteordev.org/snapshots") { name = "Meteor Snapshots" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    loomx.loomVersion = "1.18-SNAPSHOT"

    create(rootProject) {
        versions("1.21.11", "26.1.2", "26.3")
        vcsVersion = "26.3"
    }
}

rootProject.name = "FutureAds"
