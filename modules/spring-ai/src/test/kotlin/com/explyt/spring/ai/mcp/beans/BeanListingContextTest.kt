/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.explyt.spring.ai.mcp.SpringBootApplicationMcpToolset
import com.explyt.spring.core.externalsystem.model.BeanSearch
import com.explyt.spring.core.externalsystem.model.SpringBeanData
import com.explyt.spring.core.externalsystem.model.SpringBeanType
import com.explyt.spring.core.externalsystem.setting.NativeProjectSettings
import com.explyt.spring.core.externalsystem.setting.NativeSettings
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.service.beans.NativeBeanSnapshotReader
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.internal.InternalExternalProjectInfo
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsDataStorage
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.coroutines.runBlocking

/**
 * The existing listing, now answering from the same chosen model the lookup tool uses.
 *
 * What is pinned here is the part a shared model could quietly change: which application answers, what happens
 * when the choice is ambiguous, and the flat one-row-per-name enumeration callers already depend on.
 */
class BeanListingContextTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "mcp/"

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()
    private val linkedPaths = mutableListOf<String>()

    override fun tearDown() {
        try {
            val settings = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
            linkedPaths.forEach { settings.unlinkExternalProject(it) }
            linkedPaths.clear()
        } finally {
            super.tearDown()
        }
    }

    /**
     * Every name a bean answers to is its own row.
     *
     * The lookup tool counts `@Bean(name = {"systemClock", "utcClock"})` as one identity; this listing has
     * always enumerated names, and moving it onto the shared model must not quietly change that to identities.
     */
    fun testAliasesStayOneRowPerName() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("beanQuery", "")

        val beans = listing(beanType = "COMPONENT")

        val clocks = beans.filter { it["className"].asText() == CLOCK }
        assertEquals(
            "Both declared names must be listed, got ${names(beans)}",
            setOf("systemClock", "utcClock"),
            clocks.map { it["beanName"].asText() }.toSet()
        )
        assertTrue("Every row carries the same declaring module", clocks.all { it["moduleName"].asText() == module.name })
    }

    /**
     * A bean a loaded context reports without project sources keeps its row with an empty module.
     *
     * Dropping it would report a bean that exists in the application as absent, which is the failure the empty
     * string exists to avoid.
     */
    fun testABeanWithNoProjectModuleKeepsItsRow() = runBlocking<Unit> {
        installNativeRoot(applicationClass(), "libraryClock", CLOCK)
        assertEquals(
            "Precondition: the root must be loaded, otherwise nothing is proven",
            1, NativeBeanSnapshotReader(project).contexts().size
        )

        val beans = listing(beanType = "COMPONENT", source = "NATIVE")

        val clock = beans.firstOrNull { it["beanName"].asText() == "libraryClock" }
        assertNotNull("A bean without project sources must still be listed, got ${names(beans)}", clock)
        assertEquals("", clock!!["moduleName"].asText())
        assertEquals(CLOCK, clock["className"].asText())
    }

    /** Two loaded roots for one application are a question for the caller, not a merged answer. */
    fun testAmbiguousContextIsReportedInsteadOfMerged() = runBlocking<Unit> {
        val application = applicationClass()
        installNativeRoot(application, "clockOne", CLOCK, suffix = "one")
        installNativeRoot(application, "clockTwo", CLOCK, suffix = "two")
        assertEquals(
            "Precondition: both roots must be loaded",
            2, NativeBeanSnapshotReader(project).contexts().size
        )

        try {
            listing(beanType = "COMPONENT", source = "NATIVE")
            fail("An ambiguous context must not be answered by merging the roots")
        } catch (e: Exception) {
            assertTrue(
                "The failure must name what to disambiguate, got: ${e.message}",
                e.message?.contains("CONTEXT_REQUIRED") == true
            )
        }
    }

    /** An unknown source is refused rather than silently treated as the default. */
    fun testUnknownSourceIsRefused() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("beanQuery", "")

        try {
            listing(beanType = "COMPONENT", source = "LIVE")
            fail("An unknown source must be refused")
        } catch (e: Exception) {
            assertTrue("The failure must name the rejected source, got: ${e.message}", e.message?.contains("LIVE") == true)
        }
    }

    /**
     * A supplied path that names no open project is refused, even though the class name would have found one.
     *
     * The class-name search exists to disambiguate an omitted path; letting it rescue a supplied one would
     * answer about a codebase the caller did not name, which is exactly what a wrong path looks like.
     */
    fun testASuppliedPathIsNotRescuedByTheClassNameSearch() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("beanQuery", "")

        try {
            listing(beanType = "COMPONENT", projectPath = "/nowhere/at/all")
            fail("A path naming no open project must be refused, not resolved by application class name")
        } catch (e: Exception) {
            assertTrue(
                "The failure must name the path that matched nothing, got: ${e.message}",
                e.message?.contains("/nowhere/at/all") == true
            )
        }
    }

    /** With one project open, an omitted path is the question the resolver answers rather than refuses. */
    fun testAnOmittedPathIsAnsweredFromTheOnlyOpenProject() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("beanQuery", "")

        val beans = listing(beanType = "COMPONENT", projectPath = null)

        assertTrue("The listing must answer without a path", names(beans).contains("systemClock"))
    }

    private suspend fun listing(
        beanType: String,
        source: String = "STATIC",
        contextId: String? = null,
        projectPath: String? = project.basePath!!
    ): List<JsonNode> = mapper.readTree(
        toolset.applicationBeans(
            applicationClassName = APPLICATION,
            projectPath = projectPath,
            beanType = beanType,
            source = source,
            contextId = contextId
        )
    ).toList()

    private fun names(beans: List<JsonNode>): List<String> = beans.map { it["beanName"].asText() }

    private fun applicationClass(): PsiClass {
        myFixture.addFileToProject(
            "com/explyt/demo/App.java",
            """
            package com.explyt.demo;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class App {
            }
            """.trimIndent()
        )
        return JavaPsiFacade.getInstance(project)
            .findClass(APPLICATION, GlobalSearchScope.projectScope(project))
            ?: error("No PSI for $APPLICATION")
    }

    private fun installNativeRoot(application: PsiClass, beanName: String, type: String, suffix: String = "") {
        val base = application.navigationElement.containingFile.virtualFile.canonicalPath!!
        val path = base + suffix
        project.getService(NativeSettings::class.java).linkProject(NativeProjectSettings().apply {
            externalProjectPath = path
            qualifiedMainClassName = application.qualifiedName
        })
        linkedPaths += path

        val root = DataNode(
            ProjectKeys.PROJECT,
            ProjectData(SYSTEM_ID, application.name!! + suffix, project.basePath!!, path),
            null
        )
        root.createChild(BeanSearch.KEY, BeanSearch(true, path))
        root.createChild(
            SpringBeanData.KEY,
            SpringBeanData(beanName, type, "singleton", null, null, SpringBeanType.OTHER, true, true, false)
        )
        ExternalProjectsDataStorage.getInstance(project)
            .update(InternalExternalProjectInfo(SYSTEM_ID, path, root))
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }

    private companion object {
        const val APPLICATION = "com.explyt.demo.App"
        const val CLOCK = "java.time.Clock"
    }
}
