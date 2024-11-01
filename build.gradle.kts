import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
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

tasks.register("runIde") {
    doFirst {
        throw GradleException("Use project specific runIde command, i.e. :spring-bootstrap:runIde")
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.intellij.platform")

    // This syntax is used to avoid duplicated in compileKotlin and compileTestKotlin settings
    //noinspection GroovyAssignabilityCheck

    java.toolchain.languageVersion = JavaLanguageVersion.of(21)

    tasks.withType<KotlinJvmCompile>().configureEach {
        kotlinOptions {
            jvmTarget = java.toolchain.languageVersion.get().toString()
            freeCompilerArgs += listOf("-Xjvm-default=all-compatibility", "-Xjsr305=strict")
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
}