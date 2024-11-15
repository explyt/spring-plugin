/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
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
    implementation("io.sentry:sentry:7.12.1")
    intellijPlatform {
        instrumentationTools()

        create(defaultIdeaType, defaultIdeaVersion, useInstaller = false)
        jetbrainsRuntime()
        bundledPlugins(intellijPlugins)
    }
}

kotlin {
    jvmToolchain(21)
}