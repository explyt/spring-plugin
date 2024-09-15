import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.date
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask
import proguard.gradle.ProGuardTask
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files

plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
    //id("org.jetbrains.intellij.platform.migration")
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
evaluationDependsOn(":spring-aop")
evaluationDependsOn(":jpa")
evaluationDependsOn(":test-framework")
evaluationDependsOn(":llm-integration")

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
    val ideaPlatformVersion = "${rootProject.ext["sinceVersion"]}".substring(0, 3)
    return "${ideaPlatformVersion}.${rootProject.ext["pluginVersion"]}.${ext["snapshotVersion"]}"
    //return "2024.${ideaPlatformVersion}.${ext["snapshotVersion"]}"
}.invoke()

val springPluginName = "spring-tool"
ext["springPluginName"] = springPluginName

val distFilePostfix = rootProject.optProperty("distFilePostfix") ?: version

val buildArchiveName = "${springPluginName}-${distFilePostfix}.zip"

//Add "-PlaunchUltimate" property to "run jpa buddy" run/debug configuration in arguments
val launchUltimate = rootProject.hasProperty("launchUltimate")
//Add "-Pobfuscate" to obfuscate
val obfuscate = rootProject.hasProperty("obfuscate")
val includeLlmIntegrationModule = false

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
val springAopProject = project(":spring-aop")
val jpaProject = project(":jpa")
val springBootstrapModule = project(":spring-bootstrap")
val llmIntegrationProject = project(":llm-integration")
val testFramework = project(":test-framework")


repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/explyt/ai-backend")
        credentials {
            username = "EgorkaKulikov"
            password = "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"
        }
    }
    intellijPlatform {
        defaultRepositories()
        localPlatformArtifacts()
    }
}

afterEvaluate {
    println("evaluated")
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
    implementation(springAopProject)
    implementation(jpaProject)
    if (includeLlmIntegrationModule) {
        implementation(llmIntegrationProject)
    }
    testImplementation(testFramework)
    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        instrumentationTools()
        zipSigner()

        localPlugin(project(":base"))
        localPlugin(project(":spring-core"))
        localPlugin(project(":spring-gradle"))
        localPlugin(project(":spring-data"))
        localPlugin(project(":spring-security"))
        localPlugin(project(":spring-web"))
        localPlugin(project(":spring-cloud"))
        localPlugin(project(":spring-initializr"))
        localPlugin(project(":spring-integration"))
        localPlugin(project(":spring-messaging"))
        localPlugin(project(":spring-aop"))
        localPlugin(project(":jpa"))
        if (includeLlmIntegrationModule) {
            localPlugin(project(":llm-integration"))
        }

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
        pluginDependencies += springAopProject.ext["intellijPlugins"] as Iterable<String>
        pluginDependencies += jpaProject.ext["intellijPlugins"] as Iterable<String>
        if (includeLlmIntegrationModule) {
            pluginDependencies += llmIntegrationProject.ext["intellijPlugins"] as Iterable<String>
        }

        bundledPlugins(pluginDependencies.toList())

        create("IC", rootProject.ext["defaultIdeaVersion"] as String)
//        if (launchUltimate) {
//            create("IC", rootProject.ext["defaultIdeaVersion"] as String)
//        } else {
//            create("IU", rootProject.ext["defaultIdeaVersion"] as String)
//        }
    }
}



// See https://github.com/JetBrains/gradle-intellij-plugin/
@Suppress("UNCHECKED_CAST")
intellijPlatform {
    //version = project.ext["defaultIdeaVersion"]
    pluginConfiguration {
        name = springPluginName
        version = rootProject.ext["defaultIdeaVersion"] as String
        version.set(springBootstrapModule.version as String)
        //changeNotes.set(springCoreProject.file("CHANGELOG.html").readText())
        description = rootProject.file("README.md").readText()
            .let { org.jetbrains.changelog.markdownToHTML(it) }
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
            //        untilBuild.set(optProperty("setUntilVersion") ?: "")
            untilBuild.set(rootProject.ext["untilVersion"] as String)
        }
    }

    instrumentCode = false
    buildSearchableOptions = false

    pluginVerification {
        ides {
            recommended()
        }
        //ignoreWarnings = true
        //        failureLevel.set(listOf(
        //            RunPluginVerifierTask.FailureLevel.INVALID_PLUGIN,
        //            RunPluginVerifierTask.FailureLevel.COMPATIBILITY_PROBLEMS,
        //            RunPluginVerifierTask.FailureLevel.COMPATIBILITY_WARNINGS
        //        ))

        val buildArchivePath = layout.buildDirectory.file("distributions/${buildArchiveName}")
        //        distributionFile.set(buildArchivePath.get().asFile)

    }
    // see https://plugins.jetbrains.com/docs/intellij/plugin-signing.html
    signing {
        certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
        privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    }
    publishing {
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
        val buildArchivePath = layout.buildDirectory.file("distributions/${buildArchiveName}")
        //distributionFile.set(buildArchivePath.get().asFile)
        hidden = true
    }
}

val sandboxLibPath = layout.buildDirectory.dir("idea-sandbox/plugins/${springPluginName}/lib")
val extractedDirPath = layout.buildDirectory.dir("extracted")
val obfuscatedJarPath = layout.buildDirectory.file("libs/${springPluginName}-obfuscated.jar")

fun deleteDirectory(pathToBeDeleted: java.nio.file.Path) {
    Files.walk(pathToBeDeleted)
        .sorted(Comparator.reverseOrder())
        .peek {
            println("About to path entry from ZIP File: " + it.toUri())
        }
        .forEach { Files.delete(it) };}


fun removeFromJar(pathToJar: String, fileToRemove: String) {
    /* Define ZIP File System Properties in HashMap */
    /* We want to read an existing ZIP File, so we set this to False */
    /* Specify the path to the ZIP File that you want to read as a File System */
    val zipDisk: URI = URI.create("jar:file://$pathToJar")

    FileSystems.newFileSystem(zipDisk, mapOf("create" to "false")).use { zipfs ->
        /* Get the Path inside ZIP File to delete the ZIP Entry */
        val pathInZipfile = zipfs.getPath("/$fileToRemove")
        println("About to delete an entry from ZIP File" + pathInZipfile.toUri())

        deleteDirectory(pathInZipfile)

        /* Execute Delete */
        //Files.delete(pathInZipfile)
        println("File successfully deleted")
    }
}

val extractJar by tasks.registering(Copy::class) {
    val prepareSandbox = tasks.named<PrepareSandboxTask>("prepareSandbox");
    val libDir = prepareSandbox.flatMap { it.defaultDestinationDirectory }
//    .flatMap {
//        objects.directoryProperty().fileProvider(it)
//    }
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

    // workaround for proguard bug: https://github.com/Guardsquare/proguard/issues/94
    // See: https://github.com/Guardsquare/proguard/issues/94#issuecomment-723663507
    injars(configurations.kotlinCompilerClasspath.get().filter { it.name.contains("stdlib") })

    val toFilter = configurations.compileClasspath.get().files
        .filter { it.startsWith(rootProject.layout.projectDirectory.asFile) }

    // File contains K2 files, but proguard supports up to 1.9
    val invalidFiles = configurations.compileClasspath.get().files
        .filter { it.endsWith("jetbrains.kotlinx.metadata.jvm.jar") }

    val classPath = configurations.compileClasspath.get()
        .minus(toFilter)
        .minus(invalidFiles)
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
        removeFromJar(obfuscatedJarPath.get().asFile.path, "kotlin")
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

    runIde {
        // Customize in ~/.gradle/gradle.properties:
        // runIdeXmx=1600M
        if (rootProject.hasProperty("runIdeXmx")) {
            val xmx = rootProject.property("runIdeXmx")
            jvmArgs("-Xmx$xmx")
        }
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
            springAopProject.tasks.test,
            llmIntegrationProject.tasks.test,
            jpaProject.tasks.test
        )
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


