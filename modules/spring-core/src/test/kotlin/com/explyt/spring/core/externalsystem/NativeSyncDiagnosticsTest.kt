/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem

import com.explyt.spring.core.externalsystem.process.ExplytCapturingProcessAdapter
import com.explyt.spring.core.externalsystem.utils.NativeClasspathValidator
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar

/**
 * A failed native sync must name what is actually missing. The pre-2.4 Spring Boot hint belongs only to the
 * startup classes the bean reader needs from Spring itself; every other absent class means the launch classpath
 * lost a library, and the library whose file never reached the disk is what the user has to fix.
 */
class NativeSyncDiagnosticsTest : ExplytKotlinLightTestCase() {

    fun testMissingClassNameIsParsedAsQualifiedName() {
        val line = "java.lang.NoClassDefFoundError: " +
                "org/springframework/boot/configurationmetadata/ConfigurationMetadataRepositoryJsonBuilder"

        assertEquals(
            "org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder",
            ExplytCapturingProcessAdapter.parseMissingClassName(line)
        )
    }

    /**
     * A Spring startup failure repeats the error in the `Caused by` chain and in wrapper exceptions. Only the
     * first occurrence is the root cause; a later frame must not overwrite it.
     */
    fun testFirstMissingClassWins() {
        val output = """
            java.lang.NoClassDefFoundError: org/springframework/boot/configurationmetadata/Builder
            Caused by: java.lang.NoClassDefFoundError: com/example/Later
        """.trimIndent()

        assertEquals(
            "org.springframework.boot.configurationmetadata.Builder",
            ExplytCapturingProcessAdapter.parseMissingClassName(output)
        )
    }

    fun testOutputWithoutClassErrorHasNoMissingClass() {
        val output = "Caused by: java.lang.IllegalStateException: Could not resolve placeholder 'SERVER_PORT'"

        assertNull(ExplytCapturingProcessAdapter.parseMissingClassName(output))
    }

    fun testSpringStartupClassesSelectOldBootMessage() {
        for (startupClass in listOf(
            "org.springframework.core.metrics.ApplicationStartup",
            "org.springframework.core.metrics.StartupStep"
        )) {
            val message = missingClassMessage(startupClass, emptyList())

            assertTrue(
                "$startupClass must keep the pre-2.4 Spring Boot hint, but was: $message",
                message.contains("2.4.0") && message.contains("explyt.spring.native.old")
            )
        }
    }

    fun testUnrelatedMissingClassIsNamedInsteadOfBlamingBootVersion() {
        val missingClassName = "org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder"

        val message = missingClassMessage(missingClassName, emptyList())

        assertTrue("the message must name the missing class, but was: $message", message.contains(missingClassName))
        assertFalse("an incomplete classpath must not be reported as an old Boot, but was: $message", message.contains("2.4.0"))
    }

    fun testMissingLibrariesAreNamedInTheMessage() {
        val missingClassName = "org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder"
        val libraryName = "Gradle: org.springframework.boot:spring-boot-configuration-metadata:3.5.9"

        val message = missingClassMessage(missingClassName, listOf(libraryName))

        assertTrue("the message must name the missing class, but was: $message", message.contains(missingClassName))
        assertTrue("the message must name the absent library, but was: $message", message.contains(libraryName))
    }

    fun testLibraryWithoutFileOnDiskIsReported() {
        val libraryName = "Gradle: org.springframework.boot:spring-boot-configuration-metadata:3.5.9"
        addLibrary(libraryName, "jar://${myFixture.tempDirPath}/does-not-exist.jar!/")
        val addedLibrary = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
            .getLibraryByName(libraryName) as LibraryEx
        assertTrue(
            "fixture is vacuous unless the platform treats the root as invalid",
            addedLibrary.getInvalidRootUrls(OrderRootType.CLASSES).isNotEmpty()
        )

        val librariesWithMissingFiles =
            NativeClasspathValidator.findLibrariesWithMissingFiles(arrayOf(myFixture.module))

        assertEquals(listOf(libraryName), librariesWithMissingFiles)
    }

    fun testLibraryWithExistingFileIsNotReported() {
        val existingJar = findAnyLibraryRootUrlOnDisk()
        addLibrary("Gradle: present", existingJar)

        val librariesWithMissingFiles =
            NativeClasspathValidator.findLibrariesWithMissingFiles(arrayOf(myFixture.module))

        assertEmpty(librariesWithMissingFiles)
    }

    private fun addLibrary(libraryName: String, classesRootUrl: String) {
        val libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
        WriteAction.runAndWait<RuntimeException> {
            val library = libraryTable.createLibrary(libraryName)
            library.modifiableModel.apply {
                addRoot(classesRootUrl, OrderRootType.CLASSES)
                commit()
            }
            ModuleRootModificationUtil.addDependency(myFixture.module, library)
        }
    }

    private fun findAnyLibraryRootUrlOnDisk(): String {
        val jarPath = System.getProperty("java.home") + "/lib/modules"
        assertTrue("the control fixture needs a file that exists", java.io.File(jarPath).exists())
        return "file://$jarPath"
    }
}
