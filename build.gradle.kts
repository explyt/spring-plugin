import org.jetbrains.intellij.tasks.PrepareSandboxTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import proguard.gradle.ProGuardTask

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.4.1")
        classpath("org.jetbrains.intellij.plugins:gradle-intellij-plugin:1.16.0")
    }
}

plugins {
    java
    id("org.jetbrains.intellij") apply false
    kotlin("jvm") apply false
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.intellij")

    repositories {
        mavenCentral()
    }

    // This syntax is used to avoid duplicated in compileKotlin and compileTestKotlin settings
    //noinspection GroovyAssignabilityCheck
    tasks.withType<KotlinCompile>().all {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_17.toString()
            freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
        }
    }

    val jarTask = tasks.named("jar")

    val proGuardTask = tasks.register<ProGuardTask>("proguard") {
        configuration(file("../../proguard.pro"))

        injars(jarTask.flatMap { (it as Jar).archiveFile })
        libraryjars(configurations.compileClasspath.get())

        // Automatically handle the Java version of this build.
        if (System.getProperty("java.version").startsWith("1.")) {
            // Before Java 9, the runtime classes were packaged in a single jar file.
            libraryjars("${System.getProperty("java.home")}/lib/rt.jar")
        } else {
            // As of Java 9, the runtime classes are packaged in modular jmod files.
            //        for (final def file in new File("${System.getProperty("java.home")}/jmods/").listFiles()) {
            //            libraryjars file.getAbsolutePath(), jarfilter: "!**.jar", filter: "!module-info.class"
            //        }

            val filterArgs = mapOf(
                "jarfilter" to "!**.jar"
                , "filter" to "!module-info.class"
            )
            val jdkDependencies = listOf(
                "java.base.jmod"
                , "java.desktop.jmod"
                , "java.net.http.jmod"
                , "java.logging.jmod"
                , "java.instrument.jmod"
                , "java.datatransfer.jmod"
            )
            for (dependency in jdkDependencies) {
                libraryjars(filterArgs, "${System.getProperty("java.home")}/jmods/${dependency}")
            }
        }
        outjars(layout.buildDirectory.file("libs/${project.name}-obfuscated.jar"))
    }

    val inputPath = layout.buildDirectory.file("libs/${project.name}.jar")
    val outputPath = layout.buildDirectory.file("libs/${project.name}-obfuscated.jar")

    tasks.withType<PrepareSandboxTask> {
        if (project.name == "spring-bootstrap") {
            //dependsOn(proGuardTask)
            doFirst {
                val sourceFile = inputPath.get().asFile
                if (sourceFile.exists()) {
                    val proguarded = outputPath.get().asFile
                    if (proguarded.exists()) {
                        delete() //delete original jar
                        proguarded.renameTo(sourceFile)
                        println("Plugin archive successfully obfuscated and optimized.")
                    } else {
                        println("ProGuarded file doesn't exist.")
                    }
                } else {
                    println("Original file doesn't exist.")
                }
            }
        }
    }

}