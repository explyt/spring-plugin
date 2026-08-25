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
        "org.jetbrains.kotlin"
)
ext {
    set("intellijPlugins", intellijPlugins)
}

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
    implementation("io.sentry:sentry:8.29.0")
    intellijPlatform {
        instrumentationTools()

        create(defaultIdeaType, defaultIdeaVersion, useInstaller = false)
        jetbrainsRuntime()
        bundledPlugins(intellijPlugins)
    }
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
}

kotlin {
    jvmToolchain(21)
}