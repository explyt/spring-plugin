rootProject.name = "esprito-studio"

pluginManagement {
    val kotlin_version: String by settings
    val gradle_intellij_plugin_version: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlin_version
        id("org.jetbrains.intellij") version gradle_intellij_plugin_version
    }
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
includeProject("spring-bootstrap", "modules/spring-bootstrap")
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