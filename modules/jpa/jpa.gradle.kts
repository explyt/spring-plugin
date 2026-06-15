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

val intellijPlugins = listOf(
        "com.intellij.java",
        "org.jetbrains.kotlin"
)
ext {
    set("intellijPlugins", intellijPlugins)
}

evaluationDependsOn(":base")

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

tasks {
    withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:-serial")
    }
}

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
val testFramework = project(":test-framework")

val defaultIdeaType: String by rootProject
val defaultIdeaVersion: String by rootProject

dependencies {
    implementation(baseProject)
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