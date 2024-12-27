import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "explyt-spring"

pluginManagement {
    val kotlinVersion: String by settings
    val gradleIntellijPluginVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion apply false
        id("org.jetbrains.intellij.platform") version gradleIntellijPluginVersion apply false
        id("org.jetbrains.intellij.platform.module") version gradleIntellijPluginVersion apply false
        id("org.jetbrains.intellij.platform.migration") version gradleIntellijPluginVersion apply false
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
    id("org.jetbrains.intellij.platform.settings") version "2.1.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenLocal()
        mavenCentral()

        intellijPlatform {
            defaultRepositories()
            jetbrainsRuntime()
            localPlatformArtifacts()
        }

    }
}

includeProject("base")
includeProject("spring-core")
includeProject("spring-data")
includeProject("spring-security")
includeProject("spring-web")
includeProject("spring-cloud")
includeProject("spring-initializr")
includeProject("spring-integration")
includeProject("spring-messaging")
includeProject("spring-aop")
includeProject("jpa")
includeProject("test-framework")
includeProject("spring-gradle")
includeProject("spring-bootstrap")

fun includeProject(name: String, path: String = "modules/$name") {
    include(":$name")
    project(":$name").projectDir = File(settingsDir, path)

    project(":$name").buildFileName = "${name}.gradle.kts"
}