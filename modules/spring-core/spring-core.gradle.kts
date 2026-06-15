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

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

val intellijPlugins = listOf(
    "com.intellij.java",
    "com.intellij.properties",
    //"org.intellij.intelliLang",
    "org.jetbrains.plugins.yaml",
    "org.jetbrains.kotlin",
    "org.jetbrains.plugins.terminal",
    "com.jetbrains.sh",
    "com.intellij.modules.json"
)

ext {
    set("intellijPlugins", intellijPlugins)
}

evaluationDependsOn(":base")

intellijPlatform {
    buildSearchableOptions = false
    instrumentCode = false
}

sourceSets {
    main {
        java {
            srcDirs("src/main/gen")
        }
    }
}

tasks {
    runIde { enabled = false }
    publishPlugin { enabled = false }
    verifyPlugin { enabled = false }
}

val baseProject = project(":base")

val defaultIdeaType: String by rootProject
val defaultIdeaVersion: String by rootProject

dependencies {
    api(baseProject)
    implementation("com.cronutils:cron-utils:9.2.1")
    implementation("javax.validation:validation-api:2.0.1.Final")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-properties:2.19.0") {
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-databind") // it is already exist in IDEA.jar
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-core")
    }
    implementation(fileTree("libs") { include("*.jar") })

    intellijPlatform {
        intellijIdea(defaultIdeaVersion) {
            useInstaller = false
        }
        bundledPlugins(intellijPlugins)
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
    testImplementation(project(":test-framework"))
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
}

kotlin {
    jvmToolchain(21)
}