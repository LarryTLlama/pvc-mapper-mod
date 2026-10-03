pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    id("dev.kikugie.loom-back-compat") version "0.4.2"
}

// Read supported versions from root gradle.properties
val stonecutterVersions = (providers.gradleProperty("stonecutter_versions").getOrNull() ?: "1.21.10, 1.21.11, 26.1, 26.2")
    .split(",")
    .map { it.trim() }

stonecutter {
    create(rootProject) {
        versions(stonecutterVersions)
        vcsVersion = "1.21.10"
    }
}

rootProject.name = "pvc-mapper-mod"