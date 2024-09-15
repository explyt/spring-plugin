plugins {
    java
    id("org.jetbrains.intellij.platform")
    kotlin("jvm")
    id("io.sentry.jvm.gradle") version("4.10.0")
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

        create(defaultIdeaType, defaultIdeaVersion)
        bundledPlugins(intellijPlugins)
    }
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

sentry {
    // Generates a JVM (Java, Kotlin, etc.) source bundle and uploads your source code to Sentry.
    // This enables source context, allowing you to see your source
    // code as part of your stack traces in Sentry.
    includeSourceContext = true

    org = "ilya-muromtsev"
    projectName = "esprito-plugin"
    authToken = "sntrys_eyJpYXQiOjE3MjE2ODAyODQuMDExMTI5LCJ1cmwiOiJodHRwczovL3NlbnRyeS5pbyIsInJlZ2lvbl91cmwiOiJodHRwczovL2RlLnNlbnRyeS5pbyIsIm9yZyI6ImlseWEtbXVyb210c2V2In0"
}

//sourceSets {
//    main.kotlin.srcDirs += "src/main/kotlin"
//}
