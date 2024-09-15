import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    id("org.jetbrains.intellij.platform")
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
    implementation("com.vladsch.flexmark:flexmark:0.64.8")
    implementation("com.explyt.ai-backend:ai-backend:v1.7.0")

    //----jar hell with ai-backend
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:2.17.1")
    //----jar hell with ai-backend

    intellijPlatform {
        create(defaultIdeaType, defaultIdeaVersion)
        bundledPlugins(intellijPlugins)
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
    testImplementation(project(":test-framework"))
    testImplementation("junit:junit:4.13.2")
}

configurations.named("testRuntimeClasspath").get().apply {
    exclude(group = "org.codehaus.groovy", module = "groovy")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/explyt/ai-backend")
        credentials {
            username = "EgorkaKulikov"
            password = "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"
        }
    }
    mavenLocal()
    intellijPlatform {
        defaultRepositories()
        localPlatformArtifacts()
    }
}

kotlin {
    jvmToolchain(21)
}