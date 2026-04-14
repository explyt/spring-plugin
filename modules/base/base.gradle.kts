/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    java
    id("org.jetbrains.intellij.platform.module")
    kotlin("jvm")
}

val intellijPlugins = listOf(
        "com.intellij.java",
        "org.jetbrains.kotlin",
        "com.intellij.modules.json",
)
ext {
    set("intellijPlugins", intellijPlugins)
}

evaluationDependsOn(":test-framework")

val defaultIdeaType: String by rootProject
val defaultIdeaVersion: String by rootProject

intellijPlatform {
    pluginConfiguration {
        version = defaultIdeaVersion
    }
    buildSearchableOptions = false
    instrumentCode = true
}

tasks {
    runIde { enabled = false }
    publishPlugin { enabled = false }
    verifyPlugin { enabled = false }
}


dependencies {
    implementation("io.sentry:sentry:1.7.30") {
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-core")
    }
    intellijPlatform {
        intellijIdea(defaultIdeaVersion) {
            useInstaller = false
        }
        bundledPlugins(intellijPlugins)
    }
}

kotlin {
    jvmToolchain(21)
}