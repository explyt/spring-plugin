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
    implementation("io.sentry:sentry:8.29.0")
    intellijPlatform {
        create(defaultIdeaType, defaultIdeaVersion, useInstaller = false)
        jetbrainsRuntime()
        bundledPlugins(intellijPlugins)
    }
    testImplementation("junit:junit:4.13.2")
    // Both versions are explicit because this module declares no JUnit BOM, so a versionless coordinate
    // has nothing to resolve against. They are floors: a platform line that already supplies a newer
    // JUnit wins the conflict. The launcher is named because Gradle 9 stopped adding it implicitly, and
    // these are plain JUnit tests rather than IntelliJ fixtures that would drag it in transitively.
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

kotlin {
    jvmToolchain(21)
}