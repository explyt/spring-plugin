import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.date
import org.jetbrains.intellij.platform.gradle.Constants
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask
import proguard.gradle.ProGuardTask
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files

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
    "jpa"
)

allCompileProjects.forEach {
    evaluationDependsOn(":$it")
}
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
                useVersion("1.9.24")
                because("resolve kotlin version conflicts in favor of local version catalog")
            }
        }
    }

    testRuntimeClasspath {
        exclude(group = "org.codehaus.groovy", module = "groovy")
    }
}

dependencies {
    allCompileProjects.forEach {
        implementation(project(":$it"))
    }
    testImplementation(testFramework)
    testImplementation("junit:junit:4.13.2")

    @Suppress("UNCHECKED_CAST")
    intellijPlatform {
        instrumentationTools()
        zipSigner()

        val pluginDependencies = mutableSetOf<String>()
        allCompileProjects.forEach {
            pluginDependencies += project.project(":$it").ext["intellijPlugins"] as Iterable<String>
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
        version.set(springBootstrapModule.version as String)
        //changeNotes.set(springCoreProject.file("CHANGELOG.html").readText())
        description.set(rootProject.file("README.md").readText()
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
            //        untilBuild.set(optProperty("setUntilVersion") ?: "")
            untilBuild.set(rootProject.ext["untilVersion"] as String)
        }
    }

    instrumentCode.set(false)
    buildSearchableOptions.set(false)

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

val extractedDirPath = layout.buildDirectory.dir("extracted")
val extractedLibDepsPath = layout.buildDirectory.dir("libdeps")
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
    val prepareSandbox = tasks.named<PrepareSandboxTask>(Constants.Tasks.PREPARE_SANDBOX);
    val libDir = prepareSandbox.flatMap { it.defaultDestinationDirectory }
    val outputDir = extractedDirPath
    doFirst {
        val dir = outputDir.get().asFile
        if (dir.exists()) {
            val result = delete(dir)
            logger.info("Folder $dir is deleted: $result")
        }
    }

    val zipFiles = libDir.map {
        val allJars = it.asFileTree
            .matching { include("**/*.jar") }
            .files

        val bootBeanReader = allJars.filter { it.endsWith("explyt-spring-boot-bean-reader-0.1.jar") }.toSet()

        allJars
            .minus(bootBeanReader)
            .map { zipTree(it) }
    }

    from(zipFiles)
    into(outputDir)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    if (obfuscate) {
        outputs.doNotCacheIf("Cache is disable") { true }
    }
    doLast {
        copy {
            from(libDir.map {
                it.asFileTree.matching { include("**/explyt-spring-boot-bean-reader-0.1.jar")}.files
            })
            into(extractedLibDepsPath)
        }
    }
}

val proGuardTask by tasks.registering(ProGuardTask::class) {
    if (!obfuscate) {
        return@registering
    }
    val prepareSandbox = tasks.named<PrepareSandboxTask>(Constants.Tasks.PREPARE_SANDBOX);
    val libDir = prepareSandbox.flatMap { it.pluginDirectory }.map { it.dir("lib") }

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
    libraryjars(extractedLibDepsPath.get().asFileTree.files)

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
        delete(libDir)
        copy {
            from(obfuscatedJarPath)
            from(extractedLibDepsPath.get().asFileTree.files)
            into(libDir)
        }
        delete(obfuscatedJarPath)
        delete(extractedLibDepsPath)
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
            allCompileProjects.map { project.project(":$it").tasks.test }.toTypedArray()
        )
    }

    buildPlugin {
        if (obfuscate) {
            dependsOn(proGuardTask)
        }

        archiveFileName.set(buildArchiveName)
    }

    prepareSandbox {
        if (obfuscate) {
            delete(extractedDirPath)
            outputs.doNotCacheIf("Cache is disable") { true }
            finalizedBy(extractJar, proGuardTask)
        }
    }

    composedJar {
    }

}


