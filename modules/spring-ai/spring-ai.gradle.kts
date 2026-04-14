/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    id("org.jetbrains.intellij.platform.module")
    kotlin("jvm")
}

evaluationDependsOn(":spring-core")
evaluationDependsOn(":spring-web")

val intellijPlugins = listOf<String>()

ext {
    set("intellijPlugins", intellijPlugins)
}

val springCoreProject = project(":spring-core")
val springWebProject = project(":spring-web")

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
    implementation(springWebProject)

    intellijPlatform {
        intellijIdea(defaultIdeaVersion) {
            useInstaller = false
        }
        bundledPlugins(springCoreProject.ext["intellijPlugins"] as List<String> + intellijPlugins)
        bundledPlugin("com.intellij.mcpServer")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)

        plugin("com.explyt.test", "4.1.3-IJ-251")
    }
    testImplementation(project(":test-framework"))
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
}

kotlin {
    jvmToolchain(21)
}