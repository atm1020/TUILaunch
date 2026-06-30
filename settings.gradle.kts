import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "TUILaunch"

pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.4.0"
        id("org.jetbrains.changelog") version "2.5.0"
        id("org.jetbrains.qodana") version "2026.1.3"
        id("org.jetbrains.kotlinx.kover") version "0.9.8"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.intellij.platform.settings") version "2.17.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
    }
}
