@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://plugins.gradle.org/m2/")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.kotlin.android") {
                useVersion("1.9.0")
            }
            if (requested.id.id == "com.android.application") {
                useVersion("8.1.0")
            }
            if (requested.id.id == "com.google.devtools.ksp") {
                useVersion("1.9.0-1.0.13")
            }
            if (requested.id.id == "org.jetbrains.kotlin.plugin.serialization") {
                useVersion("1.9.0")
            }
            if (requested.id.id == "org.jetbrains.kotlin.plugin.compose") {
                useVersion("1.9.0")
            }
            if (requested.id.id == "com.google.dagger.hilt.android") {
                useVersion("2.56.2")
            }
            if (requested.id.id == "com.google.gms.google-services") {
                useVersion("4.4.2")
            }
            if (requested.id.id == "com.google.firebase.crashlytics") {
                useVersion("3.0.4")
            }
            if (requested.id.id == "com.google.firebase.firebase-perf") {
                useVersion("1.4.2")
            }
            if (requested.id.id == "androidx.navigation.safeargs.kotlin") {
                useVersion("2.9.0")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        maven { url = uri("https://androidx.dev/storage/compose-compiler/repository/") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    }
}

rootProject.name = "AuraFrameFx"
include(":app")
