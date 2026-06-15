/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

import org.jetbrains.intellij.platform.gradle.Constants
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    id("org.jetbrains.intellij.platform.module")
    kotlin("jvm")
}

val intellijPlugins = listOf("com.intellij.java", "org.jetbrains.kotlin")
ext {
    set("intellijPlugins", intellijPlugins)
}

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
    implementation(kotlin("test"))
    // needed for Intellij Platform test fixtures
    // see: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#junit5-test-framework-refers-to-junit4
    // see: https://youtrack.jetbrains.com/issue/IJPL-159134/JUnit5-Test-Framework-refers-to-JUnit4-java.lang.NoClassDefFoundError-junit-framework-TestCase?_gl=1*1fobdb8*_gcl_au*MTkwNDUxNDc4LjE3MTY5MDU0Mzc.*_ga*MTc1Njc0NzE4NS4xNzA4NDE0OTQ5*_ga_9J976DJZ68*MTcyMzcxOTM5Mi45OC4xLjE3MjM3MjIzNjEuNTkuMC4w
    implementation("junit:junit:4.13.2")
    intellijPlatform {
        intellijIdea(defaultIdeaVersion) {
            useInstaller = false
        }
        bundledPlugins(intellijPlugins)
        testFramework(TestFrameworkType.Platform, configurationName = Constants.Configurations.INTELLIJ_PLATFORM_DEPENDENCIES)
        testFramework(TestFrameworkType.Plugin.Java, configurationName = Constants.Configurations.INTELLIJ_PLATFORM_DEPENDENCIES)
    }
}

