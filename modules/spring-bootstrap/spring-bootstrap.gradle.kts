import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.date
import org.jetbrains.intellij.tasks.PrepareSandboxTask
import org.jetbrains.intellij.tasks.RunPluginVerifierTask
import proguard.gradle.ProGuardTask
import java.time.LocalDate

plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij")
    id("org.jetbrains.changelog")
}

evaluationDependsOn(":base")
evaluationDependsOn(":spring-core")
evaluationDependsOn(":spring-gradle")
evaluationDependsOn(":spring-data")
evaluationDependsOn(":spring-security")
evaluationDependsOn(":spring-web")
evaluationDependsOn(":spring-cloud")
evaluationDependsOn(":spring-initializr")
evaluationDependsOn(":spring-integration")
evaluationDependsOn(":spring-messaging")
evaluationDependsOn(":jpa")
evaluationDependsOn(":test-framework")

fun Project.optProperty(prop: String): String? {
    if (this.hasProperty(prop)) {
        return this.property(prop) as String
    }
    return null;
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
    val sincePostfix = "${ext["sinceVersion"]}".substring(0, 3)
    return "${sincePostfix}.${ext["pluginVersion"]}.${LocalDate.now()}.${ext["snapshotVersion"]}"
}.invoke()

val springPluginName = "spring-tool"
ext["springPluginName"] = springPluginName

val distFilePostfix = rootProject.optProperty("distFilePostfix") ?: version

val buildArchiveName = "${springPluginName}-${distFilePostfix}.zip"

//Add "-PlaunchUltimate" property to "run jpa buddy" run/debug configuration in arguments
val launchUltimate = rootProject.hasProperty("launchUltimate")
//Add "-Pobfuscate" to obfuscate
val obfuscate = rootProject.hasProperty("obfuscate")

val baseProject = project(":base")
val springCoreProject = project(":spring-core")
val springGradleProject = project(":spring-gradle")
val springDataProject = project(":spring-data")
val springSecurityProject = project(":spring-security")
val springWebProject = project(":spring-web")
val springCloudProject = project(":spring-cloud")
val springInitializrProject = project(":spring-initializr")
val springIntegrationProject = project(":spring-integration")
val springMessagingProject = project(":spring-messaging")
val jpaProject = project(":jpa")
val springBootstrapModule = project(":spring-bootstrap")
val testFramework = project(":test-framework")

repositories {
    mavenCentral()
}

dependencies {
    implementation(baseProject)
    implementation(springCoreProject)
    implementation(springGradleProject)
    implementation(springDataProject)
    implementation(springSecurityProject)
    implementation(springWebProject)
    implementation(springCloudProject)
    implementation(springInitializrProject)
    implementation(springIntegrationProject)
    implementation(springMessagingProject)
    implementation(jpaProject)
    testImplementation(testFramework)
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
@Suppress("UNCHECKED_CAST")
intellij {
    //version = project.ext["defaultIdeaVersion"]
    pluginName.set(springPluginName)
    version.set(rootProject.ext["defaultIdeaVersion"] as String)
    val pluginDependencies = mutableSetOf<String>()
    pluginDependencies += baseProject.ext["intellijPlugins"] as Iterable<String>
    pluginDependencies += springCoreProject.ext["intellijPlugins"] as Iterable<String>
    pluginDependencies += springGradleProject.ext["intellijPlugins"] as Iterable<String>
    pluginDependencies += springDataProject.ext["intellijPlugins"] as Iterable<String>
    pluginDependencies += springSecurityProject.ext["intellijPlugins"] as Iterable<String>
    pluginDependencies += springWebProject.ext["intellijPlugins"] as Iterable<String>
    pluginDependencies += springCloudProject.ext["intellijPlugins"] as Iterable<String>
    pluginDependencies += springInitializrProject.ext["intellijPlugins"] as Iterable<String>
    pluginDependencies += springIntegrationProject.ext["intellijPlugins"] as Iterable<String>
    pluginDependencies += springMessagingProject.ext["intellijPlugins"] as Iterable<String>
    pluginDependencies += jpaProject.ext["intellijPlugins"] as Iterable<String>
    plugins.set(pluginDependencies)
    downloadSources.set(true)
    if (launchUltimate) {
        type.set("IU")
    }
    instrumentCode.set(false)
}

val sandboxLibPath = layout.buildDirectory.dir("idea-sandbox/plugins/${springPluginName}/lib")
val extractedDirPath = layout.buildDirectory.dir("extracted")
val obfuscatedJarPath = layout.buildDirectory.file("libs/${springPluginName}-obfuscated.jar")

val extractJar by tasks.registering(Copy::class) {
    val prepareSandbox = tasks.named<PrepareSandboxTask>("prepareSandbox");
    val libDir = prepareSandbox.map { it.defaultDestinationDir }.flatMap {
        objects.directoryProperty().fileProvider(it)
    }
    val outputDir = extractedDirPath
    doFirst {
        val dir = outputDir.get().asFile
        if (dir.exists()) {
            val result = delete(dir)
            logger.info("Folder $dir is deleted: $result")
        }
    }

    val zipFiles = libDir.map {
        it.asFileTree
            .matching { include("**/*.jar") }
            .files
            .map { zipTree(it) }
    }

    from(zipFiles)
    into(outputDir)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    if (obfuscate) {
        outputs.doNotCacheIf("Cache is disable") { true }
    }
}

val proGuardTask by tasks.registering(ProGuardTask::class) {
    if (!obfuscate) {
        return@registering
    }
    configuration(file("../../proguard.pro"))

    injars(extractJar.map { it.outputs.files.singleFile })
    val toFilter = configurations.compileClasspath.get().files
        .filter { it.startsWith(rootProject.layout.projectDirectory.asFile) }

    val classPath = configurations.compileClasspath.get().minus(toFilter)
    libraryjars(classPath)

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
        , "java.rmi.jmod"
        , "java.management.jmod"
    )
    for (dependency in jdkDependencies) {
        libraryjars(filterArgs, "${System.getProperty("java.home")}/jmods/${dependency}")
    }

    outjars(obfuscatedJarPath)
    val sourceMap = layout.buildDirectory.file("distributions/source.map")
    printmapping(sourceMap)
    outputs.files.files.add(obfuscatedJarPath.get().asFile)
    outputs.files.files.add(sourceMap.get().asFile)
    outputs.files.files.add(extractedDirPath.get().asFile)
    doLast {
        delete(extractedDirPath)
        delete(sandboxLibPath)
        copy {
            from(obfuscatedJarPath)
            into(sandboxLibPath)
        }
        delete(obfuscatedJarPath)
    }
}


changelog {
    version.set(springBootstrapModule.version as String)
    path.set(rootProject.file("CHANGELOG.md").canonicalPath)
    header.set(provider { "[${version.get()}] - ${date()}" })
    //headerParserRegex.set("""(\d+\.\d+)""".toRegex())
    introduction.set(
        rootProject.file("README.md").readText()
    )
    itemPrefix.set("-")
    keepUnreleasedSection.set(true)
    unreleasedTerm.set("[Unreleased]")
    //groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
    groups.empty()
    lineSeparator.set("\n")
    combinePreReleases.set(true)
    //sectionUrlBuilder.set(ChangelogSectionUrlBuilder { repositoryUrl, currentVersion, previousVersion, isUnreleased -> "foo" })

}

tasks {
    buildSearchableOptions {
        enabled = false
    }

    patchPluginXml {
        version.set(springBootstrapModule.version as String)
        sinceBuild.set(ext["sinceVersion"] as String)
        //        untilBuild.set(optProperty("setUntilVersion") ?: "")
        untilBuild.set(ext["untilVersion"] as String)
        //changeNotes.set(springCoreProject.file("CHANGELOG.html").readText())
        changeNotes.set(provider {
            changelog.renderItem(
                changelog
                    .getUnreleased()
                    .withHeader(false)
                    .withEmptySections(false),
                Changelog.OutputType.HTML
            )
        })

    }

    runIde {
        // Customize in ~/.gradle/gradle.properties:
        // runIdeXmx=1600M
        if (rootProject.hasProperty("runIdeXmx")) {
            val xmx = rootProject.property("runIdeXmx")
            jvmArgs("-Xmx$xmx")
        }
    }

    runPluginVerifier {
        ideVersions.set(listOf(ext["pluginVerifierIdeVersion"] as String))
        failureLevel.set(listOf(
            RunPluginVerifierTask.FailureLevel.INVALID_PLUGIN,
            RunPluginVerifierTask.FailureLevel.COMPATIBILITY_PROBLEMS,
            RunPluginVerifierTask.FailureLevel.COMPATIBILITY_WARNINGS
        ))

        val buildArchivePath = layout.buildDirectory.file("distributions/${buildArchiveName}")
        distributionFile.set(buildArchivePath.get().asFile)
    }

    test {
        dependsOn(
            springCoreProject.tasks.test,
            springGradleProject.tasks.test,
            springDataProject.tasks.test,
            springSecurityProject.tasks.test,
            springWebProject.tasks.test,
            springCloudProject.tasks.test,
            springInitializrProject.tasks.test,
            springIntegrationProject.tasks.test,
            springMessagingProject.tasks.test,
            jpaProject.tasks.test
        )
    }

    // see https://plugins.jetbrains.com/docs/intellij/plugin-signing.html
    signPlugin {
        certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
        privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
        val buildArchivePath = layout.buildDirectory.file("distributions/${buildArchiveName}")
        distributionFile.set(buildArchivePath.get().asFile)
        hidden = true
    }

    //TODO
    //if (rootProject.hasProperty("pluginRepoToken") && rootProject.hasProperty("pluginRepoChannel")) {
    //    publishPlugin {
    //        token.set(pluginRepoToken)
    //        channels.set([pluginRepoChannel])
    //        if (rootProject.hasProperty("pluginDistributionFile")) {
    //            distributionFile.set(pluginDistributionFile)
    //        }
    //    }
    //}


    buildPlugin {
        archiveFileName = buildArchiveName
    }

    prepareSandbox {
        if (obfuscate) {
            delete(extractedDirPath)
            outputs.doNotCacheIf("Cache is disable") { true }
            finalizedBy(extractJar, proGuardTask)
        }
    }

}


