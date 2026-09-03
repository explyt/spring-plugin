/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    id("org.jetbrains.intellij.platform.module")
    kotlin("jvm")
}

evaluationDependsOn(":spring-core")

// JCEF moved to a separate bundled plugin in IntelliJ Platform 2026.2
val intellijPlugins = listOf("com.intellij.modules.jcef")

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
        intellijIdea(defaultIdeaVersion) {
            useInstaller = false
        }
        bundledPlugins(springCoreProject.ext["intellijPlugins"] as List<String> + intellijPlugins)
        // Without a test framework the `test` task cannot resolve the IDE distribution it runs against and fails
        // with "Cannot resolve 'product-info.json'", which is why this module had no tests at all.
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
}

kotlin {
    jvmToolchain(25)
}