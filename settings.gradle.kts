rootProject.name = "esprito-studio"

pluginManagement {
    val kotlinVersion: String by settings
    val gradleIntellijPluginVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion apply false
        id("org.jetbrains.intellij") version gradleIntellijPluginVersion apply false
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

includeProject("base", "modules/base")
includeProject("spring-core", "modules/spring-core")
includeProject("spring-data", "modules/spring-data")
includeProject("spring-security", "modules/spring-security")
includeProject("spring-web", "modules/spring-web")
includeProject("spring-cloud", "modules/spring-cloud")
includeProject("spring-initializr", "modules/spring-initializr")
includeProject("spring-integration", "modules/spring-integration")
includeProject("spring-messaging", "modules/spring-messaging")
includeProject("spring-bootstrap", "modules/spring-bootstrap", true, true)
includeProject("jpa", "modules/jpa")
includeProject("test-framework", "modules/test-framework")
includeProject("spring-gradle", "modules/spring-gradle")

fun includeProject(name: String, path: String, changeBuildFileName: Boolean = true, ktScript: Boolean = false) {
    include(":$name")
    project(":$name").projectDir = File(settingsDir, "$path")

    if (changeBuildFileName) {
        project(":$name").buildFileName = "${name}.gradle" + if (ktScript) ".kts" else ""
    }
}