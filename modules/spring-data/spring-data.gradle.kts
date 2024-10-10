import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    id("org.jetbrains.intellij.platform.module")
    kotlin("jvm")
}

evaluationDependsOn(":spring-core")
evaluationDependsOn(":jpa")

val intellijPlugins = listOf<String>()

ext {
    set("intellijPlugins", intellijPlugins)
}

val springCoreProject = project(":spring-core")
val jpaProject = project(":jpa")
val testFramework = project(":test-framework")

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
    implementation(jpaProject)

    intellijPlatform {
        create(defaultIdeaType, defaultIdeaVersion)
        bundledPlugins(
            springCoreProject.ext["intellijPlugins"] as List<String>
            + jpaProject.ext["intellijPlugins"] as List<String>
            + intellijPlugins
        )
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
    testImplementation(project(":test-framework"))
    testImplementation("junit:junit:4.13.2")
}
repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        localPlatformArtifacts()
    }
}

kotlin {
    jvmToolchain(21)
}