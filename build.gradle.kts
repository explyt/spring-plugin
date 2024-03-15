import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.4.2")
    }
}

plugins {
    java
    id("org.jetbrains.intellij") apply false
    id("org.jetbrains.changelog") version "2.2.0" apply false
    kotlin("jvm") version "1.9.22"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.intellij")

    repositories {
        mavenCentral()
    }

    // This syntax is used to avoid duplicated in compileKotlin and compileTestKotlin settings
    //noinspection GroovyAssignabilityCheck

    java.toolchain.languageVersion = JavaLanguageVersion.of(17)

    tasks.withType<KotlinJvmCompile>().configureEach {
        kotlinOptions {
            jvmTarget = java.toolchain.languageVersion.get().toString()
            freeCompilerArgs += listOf("-Xjvm-default=all-compatibility")
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        javaCompiler = javaToolchains.compilerFor {
            languageVersion = java.toolchain.languageVersion
        }
        sourceCompatibility = java.toolchain.languageVersion.get().toString()
    }

}
dependencies {
    implementation(kotlin("stdlib-jdk8"))
}
repositories {
    mavenCentral()
}