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
    "org.jetbrains.plugins.yaml",
    "org.jetbrains.kotlin"
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
    implementation(fileTree("libs") { include("*.jar") })

    intellijPlatform {
        create(defaultIdeaType, defaultIdeaVersion)
        bundledPlugins(intellijPlugins)
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
    testImplementation(project(":test-framework"))
    testImplementation("junit:junit:4.13.2")
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        localPlatformArtifacts()
    }
}

kotlin {
    jvmToolchain(21)
}

//sourceSets {
//    main.kotlin.srcDirs += "src/main/kotlin"
//}