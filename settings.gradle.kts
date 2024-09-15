rootProject.name = "esprito-studio"

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
includeProject("llm-integration")

fun includeProject(name: String, path: String = "modules/$name") {
    include(":$name")
    project(":$name").projectDir = File(settingsDir, path)

    project(":$name").buildFileName = "${name}.gradle.kts"
}