/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

buildscript {
    repositories {
        mavenLocal()
        maven { url = uri("https://repo1.maven.org/maven2/") }
    }

    dependencies {
        classpath("org.slf4j:slf4j-api:2.0.7")
    }
}

plugins {
    java
    id("org.jetbrains.intellij.platform") apply false
    id("org.jetbrains.intellij.platform.module") apply false
    id("org.jetbrains.changelog") version "2.2.1" apply false
    kotlin("jvm") apply false
}

tasks.register<DefaultTask>("runIde") {
    doFirst {
        throw GradleException("Use project specific runIde command, i.e. :spring-bootstrap:runIde")
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.intellij.platform")

    val junitBomVersion: String by rootProject

    dependencies {
        add("testImplementation", platform("org.junit:junit-bom:$junitBomVersion"))
        // Provide a launcher matching the JUnit BOM; otherwise Gradle injects its own
        // (older) junit-platform-launcher which is incompatible with JUnit Platform 6
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }

    // This syntax is used to avoid duplicated in compileKotlin and compileTestKotlin settings
    //noinspection GroovyAssignabilityCheck

    // IntelliJ Platform 2026.2 ships JVM 25 bytecode; compile with a matching toolchain and target
    java.toolchain.languageVersion = JavaLanguageVersion.of(25)

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
            freeCompilerArgs.addAll(listOf(
                "-Xjvm-default=all",
                "-Xjsr305=strict",
                "-opt-in=org.jetbrains.kotlin.K1Deprecation"
            ))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs = options.compilerArgs + "-Xlint:all"

        javaCompiler = javaToolchains.compilerFor {
            languageVersion = java.toolchain.languageVersion
        }
        sourceCompatibility = java.toolchain.languageVersion.get().toString()
    }

    // Since 2026.2 the bundled JetBrains Ultimate plugins (Spring, JPA, Swagger) are enabled
    // in the test runtime and clash with Explyt providers (duplicate references, different
    // inspection messages, shadowed message bundles). Disable them in the test sandbox
    // to keep tests hermetic.
    tasks.withType<org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask>()
        .matching { it.name.contains("Test") }
        .configureEach {
            disabledPlugins.addAll(
                "com.intellij.spring",
                "com.intellij.spring.boot",
                "com.intellij.spring.boot.initializr",
                "com.intellij.spring.cloud",
                "com.intellij.spring.data",
                "com.intellij.spring.integration",
                "com.intellij.spring.messaging",
                "com.intellij.spring.modulith",
                "com.intellij.spring.mvc",
                "com.intellij.spring.security",
                "com.intellij.javaee.jpa",
                "com.intellij.swagger",
                "com.intellij.javaee",
                "com.intellij.persistence",
                "com.intellij.jpa.jpb.model",
                "com.intellij.database",
                "com.intellij.microservices.jvm",
                "com.intellij.microservices.ui",
            )
        }

    // IntelliJ Platform tests share an IDEA sandbox and index; do not run test forks in parallel
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        maxParallelForks = 1
        maxHeapSize = "1024m"
        forkEvery = 0L

        // Disable JUnit parallel execution to avoid indexing/sandbox races
        systemProperty("junit.jupiter.execution.parallel.enabled", "false")

        // The Explyt AI Agent plugin dependency (spring-ai) bundles kotlinx-collections-immutable
        // 0.3.7, which shadows the 0.5.0 required by the 2026.2 kernel (rhizomedb) and hangs
        // test application startup with NoSuchMethodError: PersistentMap.putting
        classpath = classpath.filter { !it.name.startsWith("kotlinx-collections-immutable-jvm-0.3") }
    }
}
