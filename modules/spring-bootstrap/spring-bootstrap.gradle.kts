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

import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.date

plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

val allCompileProjects = listOf(
    "base",
    "spring-core",
    "spring-gradle",
    "spring-data",
    "spring-security",
    "spring-web",
    "spring-cloud",
    "spring-initializr",
    "spring-integration",
    "spring-messaging",
    "spring-aop",
    "jpa",
    "quarkus-core",
    "spring-ai",
)

allCompileProjects.forEach {
    evaluationDependsOn(":$it")
}
evaluationDependsOn(":test-framework")

fun Project.optProperty(prop: String): String? {
    if (this.hasProperty(prop)) {
        return this.property(prop) as String
    }
    return null
}

if (!hasProperty("snapshotVersion")) {
    ext["snapshotVersion"] = "SNAPSHOT"
}

version = fun(): String {
    if (rootProject.hasProperty("buildVersion")) {
        val bv = rootProject.property("buildVersion") as String
        if (bv.startsWith("spring.v.")) {
            return bv.substring(9)
        }
        if (bv.startsWith("v.")) {
            return bv.substring(2)
        }
        return bv
    }
    val ideaPlatformVersion = "${rootProject.ext["sinceVersion"]}".substring(0, 3)
    return "${ideaPlatformVersion}.${rootProject.ext["pluginVersion"]}.${ext["snapshotVersion"]}"
}.invoke()

val springPluginName = "spring-tool"
ext["springPluginName"] = springPluginName

val distFilePostfix = rootProject.optProperty("distFilePostfix") ?: version

val buildArchiveName = "${springPluginName}-${distFilePostfix}.zip"

val launchUltimate = rootProject.hasProperty("launchUltimate")

val springBootstrapModule = project(":spring-bootstrap")
val testFramework = project(":test-framework")


configurations {
    runtimeClasspath {
        // IDE provides Kotlin
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
    }

    configureEach {
        // IDE provides netty
        exclude("io.netty")

        if (name.startsWith("detekt")) {
            return@configureEach
        }

        // Exclude dependencies that ship with iDE
        exclude(group = "org.slf4j")
        exclude(group = "org.jetbrains", module = "annotations")
        exclude(group = "io.ktor")
        exclude(group = "org.apache.groovy", module = "groovy")
        exclude(group = "org.codehaus.groovy", module = "groovy")
        exclude(group = "org.codehaus.groovy", module = "groovy-json")
        exclude(group = "org.apache.groovy", module = "groovy-json")

        // we want kotlinx-coroutines-debug and kotlinx-coroutines-test
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")

        exclude(group = "com.fasterxml.jackson.core", module = "jackson-core")
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-annotations")
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-databind")
        exclude(group = "com.fasterxml.jackson.module", module = "jackson-module-kotlin")

        exclude(group = "org.apache.logging.log4j", module = "log4j-slf4j2-impl")

        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-coroutines")) {
                useVersion("1.8.0")
                because("resolve kotlinx-coroutines version conflicts in favor of local version catalog")
            }

            if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin")) {
                useVersion("${rootProject.ext["kotlinVersion"]}")
                because("resolve kotlin version conflicts in favor of local version catalog")
            }
        }
    }

    testRuntimeClasspath {
        exclude(group = "org.codehaus.groovy", module = "groovy")
    }
}

val defaultIdeaType: String by rootProject
val defaultIdeaVersion: String by rootProject

dependencies {
    allCompileProjects.forEach {
        implementation(project(":$it"))
    }
    testImplementation(testFramework)
    testImplementation("junit:junit:4.13.2")

    @Suppress("UNCHECKED_CAST")
    intellijPlatform {
        zipSigner()

        val pluginDependencies = mutableSetOf<String>()
        allCompileProjects.forEach {
            pluginDependencies += project.project(":$it").ext["intellijPlugins"] as Iterable<String>
        }
        bundledPlugins(pluginDependencies.toList())

        create(defaultIdeaType, defaultIdeaVersion, useInstaller = false)
        jetbrainsRuntime()
    }
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
@Suppress("UNCHECKED_CAST")
intellijPlatform {
    pluginConfiguration {
        version.set(springBootstrapModule.version as String)
        description.set(rootProject.file("PLUGIN-DESCRIPTION.md").readText()
            .let { org.jetbrains.changelog.markdownToHTML(it) })
        changeNotes.set(provider {
            changelog.renderItem(
                changelog
                    .getUnreleased()
                    .withHeader(false)
                    .withEmptySections(false),
                Changelog.OutputType.HTML
            )
        })
        ideaVersion {
            sinceBuild.set(rootProject.ext["sinceVersion"] as String)
            untilBuild.set(rootProject.ext["untilVersion"] as String)
        }
    }

    instrumentCode.set(false)
    buildSearchableOptions.set(false)

    pluginVerification {
        ides {
            recommended()
        }
    }
    // see https://plugins.jetbrains.com/docs/intellij/plugin-signing.html
    signing {
        certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
        privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    }
    publishing {
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
        hidden = true
    }
}

changelog {
    version.set(springBootstrapModule.version as String)
    path.set(rootProject.file("CHANGELOG.md").canonicalPath)
    header.set(provider { "[${version.get()}] - ${date()}" })
    introduction.set(
        rootProject.file("PLUGIN-DESCRIPTION.md").readText()
    )
    itemPrefix.set("-")
    keepUnreleasedSection.set(true)
    unreleasedTerm.set("[Unreleased]")
    groups.empty()
    lineSeparator.set("\n")
    combinePreReleases.set(true)
}

tasks {

    runIde {
        // Customize in ~/.gradle/gradle.properties:
        // runIdeXmx=1600M
        if (rootProject.hasProperty("runIdeXmx")) {
            val xmx = rootProject.property("runIdeXmx")
            jvmArgs("-Xmx$xmx")
        }
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf("-Didea.kotlin.plugin.use.k2=true")
        }
    }

    test {
        dependsOn(
            allCompileProjects.map { project.project(":$it").tasks.test }.toTypedArray()
        )
    }

    buildPlugin {
        archiveFileName.set(buildArchiveName)
    }
}


