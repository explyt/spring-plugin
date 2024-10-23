plugins {
    java
    id("org.jetbrains.intellij.platform.module")
    kotlin("jvm")
}

evaluationDependsOn(":spring-core")

val intellijPlugins = listOf<String>()

ext {
    set("intellijPlugins", intellijPlugins)
}

val springCoreProject = project(":spring-core")

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

intellijPlatform {
    buildSearchableOptions = false
    instrumentCode = false
}

tasks {
    runIde { enabled = false }
    publishPlugin { enabled = false }
    verifyPlugin { enabled = false }
}

val defaultIdeaType: String by rootProject
val defaultIdeaVersion: String by rootProject

dependencies {
    implementation(springCoreProject)
    intellijPlatform {
        create(defaultIdeaType, defaultIdeaVersion, useInstaller = false)
        jetbrainsRuntime()
        bundledPlugins(springCoreProject.ext["intellijPlugins"] as List<String> + intellijPlugins)
    }
}

kotlin {
    jvmToolchain(21)
}