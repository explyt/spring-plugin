/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.ai.mcp.beans.SpringBeanMcpToolset
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.addFromMaven
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Locations of files the project root does not contain: a module kept outside the project directory, a jar
 * checked in under it, and a file under it that belongs to no module. Uses the heavy
 * [JavaCodeInsightFixtureTestCase] because all of them need real local files.
 *
 * The outside module sits one directory below a sibling of the project directory, as `../shared/backend` does: a
 * `../` path resolved against the module's own content root then lands nowhere, so only a resolution against the
 * project root can read it back.
 */
class SpringBootApplicationMcpToolsetOutsideProjectRootTest : JavaCodeInsightFixtureTestCase() {

    private val toolset = SpringBootApplicationMcpToolset()
    private val beanToolset = SpringBeanMcpToolset()
    private val mapper = ObjectMapper()
    private lateinit var outsideRoot: File

    override fun tuneFixture(moduleBuilder: JavaModuleFixtureBuilder<*>) {
        moduleBuilder.addJdkVersion(LanguageLevel.JDK_21)
    }

    override fun setUp() {
        super.setUp()
        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            addFromMaven(model, TestLibrary.springWebMvc_6_0_7.mavenCoordinates, true)
            addFromMaven(model, TestLibrary.springBootAutoConfigure_3_1_1.mavenCoordinates, true)
        }
        outsideRoot = File(FileUtil.createTempDirectory("mcpOutsideRoot", null), "backend").apply { mkdirs() }
        assertFalse(
            "precondition: the content root lies outside the project directory",
            FileUtil.isAncestor(project.basePath!!, outsideRoot.path, false)
        )
        writeOutside(CONTROLLER_PATH, CONTROLLER_SOURCE)
        writeOutside("com/example/outside/OutsideApp.java", APPLICATION_SOURCE)
        writeOutside(CONFIG_PATH, CONFIG_SOURCE)
        writeOutside(CONSUMER_PATH, CONSUMER_SOURCE)
        WriteAction.runAndWait<Throwable> { LocalFileSystem.getInstance().refresh(false) }
        val rootFile = VfsUtil.findFile(outsideRoot.toPath(), true) ?: error("Content root not visible in VFS: $outsideRoot")
        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            model.addContentEntry(rootFile).addSourceFolder(rootFile, false)
        }
        WriteAction.runAndWait<Throwable> { LocalFileSystem.getInstance().refresh(false) }
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    fun testEndpointOutsideTheProjectDirectoryHasARelativePathThatRoundTrips() = runBlocking<Unit> {
        val endpoint = mapper.readTree(
            toolset.getHttpEndpoints(projectPath = project.basePath!!, controllerFilter = "OutsideController")
        )["endpoints"].single()
        val filePath = endpoint["filePath"].asText()

        assertEquals(relativePathOf(CONTROLLER_PATH), filePath)
        assertTrue("a module outside the project directory is reached by '../', got $filePath", filePath.startsWith("../"))
        assertFalse("no home directory in $filePath", filePath.contains(System.getProperty("user.home")))
        assertFalse("a project element names no library, got $endpoint", endpoint.has("library"))

        val head = mapper.readTree(
            toolset.traceCallChain(
                filePath = filePath,
                line = endpoint["line"].asInt(),
                projectPath = project.basePath!!,
                depth = 1,
                includeTests = false,
            )
        )["chain"][0]
        assertEquals("com.example.outside.OutsideController", head["className"].asText())
        assertEquals("items", head["methodName"].asText())
        assertEquals(filePath, head["filePath"].asText())
    }

    /** The bean tool reads the path the other tools report, and still the path relative to a content root. */
    fun testBeanInjectionPointOutsideTheProjectDirectoryIsResolvedByEitherRelativePath() = runBlocking<Unit> {
        val relativePath = relativePathOf(CONSUMER_PATH)
        assertTrue("precondition: the path climbs out of the project directory, got $relativePath", relativePath.startsWith("../"))
        val (line, column) = positionOf(CONSUMER_SOURCE, "ClockConsumer(Clock clock)", "ClockConsumer(Clock ".length)

        val byProjectPath = findBeanAt(relativePath, line, column)
        assertEquals("RESOLVED", byProjectPath["outcome"].asText())
        val candidate = byProjectPath["candidates"][0]
        assertEquals("systemClock", candidate["name"].asText())
        assertEquals(relativePathOf(CONFIG_PATH), candidate["declaration"]["filePath"].asText())
        assertFalse("a project declaration names no library", candidate["declaration"].has("library"))

        val byContentRootPath = findBeanAt(CONSUMER_PATH, line, column)
        assertEquals("RESOLVED", byContentRootPath["outcome"].asText())
        assertEquals("systemClock", byContentRootPath["candidates"][0]["name"].asText())
    }

    fun testJarUnderTheProjectDirectoryIsNamedByItsFile() {
        val libraryJar = OrderEnumerator.orderEntries(myFixture.module).librariesOnly().classesRoots
            .firstNotNullOfOrNull { JarFileSystem.getInstance().getVirtualFileForJar(it) }
            ?: error("precondition: the module has a jar library")
        val copied = File(project.basePath!!, "libs/${libraryJar.name}")
        FileUtil.copy(VfsUtilCore.virtualToIoFile(libraryJar), copied)
        val entry = WriteAction.computeAndWait<VirtualFile?, Throwable> {
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(copied)
            JarFileSystem.getInstance().refreshAndFindFileByPath("${FileUtil.toSystemIndependentName(copied.path)}!/")
        }?.let { root -> root.children.firstOrNull() ?: root } ?: error("precondition: the copied jar is readable")
        assertTrue("precondition: the jar lies under the project directory", entry.path.startsWith(project.basePath!!))

        assertEquals(McpSourceLocation(filePath = null, library = libraryJar.name), McpSourceLocation.of(entry, project))
    }

    fun testFileUnderTheProjectDirectoryOutsideEveryModuleIsRelativeToTheProjectRoot() {
        val note = File(project.basePath!!, "notes/README.md").apply {
            parentFile.mkdirs()
            writeText("notes")
        }
        val file = WriteAction.computeAndWait<VirtualFile?, Throwable> {
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(note)
        } ?: error("precondition: the file is visible in the VFS")
        assertFalse("precondition: the file belongs to no module", ProjectFileIndex.getInstance(project).isInContent(file))

        assertEquals(McpSourceLocation(filePath = "notes/README.md", library = null), McpSourceLocation.of(file, project))
    }

    private suspend fun findBeanAt(filePath: String, line: Int, column: Int): JsonNode = mapper.readTree(
        beanToolset.findSpringBean(
            projectPath = project.basePath!!,
            applicationClassName = "com.example.outside.OutsideApp",
            source = "STATIC",
            filePath = filePath,
            line = line,
            column = column,
        )
    )

    private fun writeOutside(relativePath: String, content: String) {
        File(outsideRoot, relativePath).apply {
            parentFile.mkdirs()
            writeText(content)
        }
    }

    private fun relativePathOf(pathInOutsideRoot: String): String =
        FileUtil.getRelativePath(File(project.basePath!!), File(outsideRoot, pathInOutsideRoot))!!
            .replace(File.separatorChar, '/')

    private fun positionOf(source: String, marker: String, offsetInMarker: Int): Pair<Int, Int> {
        val lines = source.lines()
        val lineIndex = lines.indexOfFirst { it.contains(marker) }
        assertTrue("precondition: marker '$marker' must exist in the fixture", lineIndex >= 0)
        return (lineIndex + 1) to (lines[lineIndex].indexOf(marker) + offsetInMarker + 1)
    }

    private companion object {
        const val CONTROLLER_PATH = "com/example/outside/OutsideController.java"
        const val CONFIG_PATH = "com/example/outside/TimeConfig.java"
        const val CONSUMER_PATH = "com/example/outside/ClockConsumer.java"

        val CONTROLLER_SOURCE = """
            package com.example.outside;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class OutsideController {
                @GetMapping("/outside/items")
                public String items() {
                    return "items";
                }
            }
        """.trimIndent()

        val APPLICATION_SOURCE = """
            package com.example.outside;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class OutsideApp {
            }
        """.trimIndent()

        val CONFIG_SOURCE = """
            package com.example.outside;

            import java.time.Clock;
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            public class TimeConfig {
                @Bean
                public Clock systemClock() {
                    return Clock.systemUTC();
                }
            }
        """.trimIndent()

        val CONSUMER_SOURCE = """
            package com.example.outside;

            import java.time.Clock;
            import org.springframework.stereotype.Service;

            @Service
            public class ClockConsumer {
                private final Clock clock;

                public ClockConsumer(Clock clock) {
                    this.clock = clock;
                }
            }
        """.trimIndent()
    }
}
