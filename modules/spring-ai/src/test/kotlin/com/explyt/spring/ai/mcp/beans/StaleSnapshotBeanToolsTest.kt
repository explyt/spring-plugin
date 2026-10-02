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
import com.intellij.mcpserver.McpToolset
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
import java.time.Instant

/**
 * Both bean tools over a recorded context that went stale bean by bean: one bean's class was removed after the
 * import, the bean the caller asks about is healthy.
 *
 * The answer about the healthy bean must not inherit the broken bean's uncertainty, and what the snapshot cannot
 * promise as a whole - that it is not live, and since when it is not - is stated once instead of on every row.
 */
class StaleSnapshotBeanToolsTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

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

    fun testALookupOfAHealthyTypeIsSingleDespiteAStaleBean() = runBlocking<Unit> {
        installStaleSnapshot()

        val root = find(source = "AUTO", typeFqn = ORDER_STORE)

        assertEquals("NATIVE_SNAPSHOT", root["model"]["source"].asText())
        assertEquals("SINGLE", root["outcome"].asText())
        assertEquals("COMPLETE", root["matchCompleteness"].asText())
        assertEquals(0, root["unresolvedCount"].asInt())
        assertEquals(
            "Only what the snapshot as a whole cannot promise belongs to this answer",
            listOf("NATIVE_SNAPSHOT_NOT_LIVE"),
            root["model"]["limitations"].map { it.asText() }.filter { it != "ALIASES_NOT_EXPORTED" }
        )
    }

    fun testANativeAnswerNamesWhenTheSnapshotWasImported() = runBlocking<Unit> {
        installStaleSnapshot()

        val model = find(source = "AUTO", typeFqn = ORDER_STORE)["model"]

        assertEquals(Instant.ofEpochMilli(IMPORTED_AT).toString(), model["snapshotImportedAt"].asText())
    }

    fun testAStaticAnswerHasNoImportTime() = runBlocking<Unit> {
        installStaleSnapshot()

        val model = find(source = "STATIC", typeFqn = ORDER_STORE)["model"]

        assertEquals("STATIC", model["source"].asText())
        assertFalse("A static model was never imported", model.has("snapshotImportedAt"))
    }

    /**
     * The listing states what the model cannot promise as a whole through 'source' and 'snapshotImportedAt'.
     * A row keeps only its own bean's limitations, so the stale bean is the only one that says its type is gone.
     */
    fun testTheListingNoLongerRepeatsSnapshotWideLimitationsOnEveryRow() = runBlocking<Unit> {
        installStaleSnapshot()

        val rows = listing()

        val healthy = rows.single { it["beanName"].asText() == "jdbcOrderStore" }
        val stale = rows.single { it["beanName"].asText() == "legacyReportJob" }
        assertEquals(Instant.ofEpochMilli(IMPORTED_AT).toString(), healthy["snapshotImportedAt"].asText())
        assertFalse(
            "NATIVE_SNAPSHOT_NOT_LIVE follows from 'source' and is not repeated on a row, got $healthy",
            rows.any { row -> row["limitations"]?.any { it.asText() == "NATIVE_SNAPSHOT_NOT_LIVE" } == true }
        )
        assertFalse(
            "A healthy row carries no stale-class limitation, got $healthy",
            healthy["limitations"]?.any { it.asText() == NativeBeanSnapshotReader.TYPE_NOT_ON_APPLICATION_CLASSPATH } == true
        )
        assertTrue(
            "The stale row keeps its own limitation, got $stale",
            stale["limitations"].any { it.asText() == NativeBeanSnapshotReader.TYPE_NOT_ON_APPLICATION_CLASSPATH }
        )
    }

    private suspend fun find(source: String, typeFqn: String): JsonNode = mapper.readTree(
        McpToolset.EP.extensionList.filterIsInstance<SpringBeanMcpToolset>().single().findSpringBean(
            projectPath = project.basePath!!,
            applicationClassName = APPLICATION,
            source = source,
            typeFqn = typeFqn
        )
    )

    private suspend fun listing(): List<JsonNode> = mapper.readTree(
        SpringBootApplicationMcpToolset().applicationBeans(
            applicationClassName = APPLICATION,
            projectPath = project.basePath!!,
            beanType = "COMPONENT",
            source = "NATIVE"
        )
    ).toList()

    private fun installStaleSnapshot() {
        val application = addApplication()
        val path = application.navigationElement.containingFile.virtualFile.canonicalPath!!
        project.getService(NativeSettings::class.java).linkProject(NativeProjectSettings().apply {
            externalProjectPath = path
            qualifiedMainClassName = application.qualifiedName
        })
        linkedPaths += path

        val root = DataNode(ProjectKeys.PROJECT, ProjectData(SYSTEM_ID, "demo", project.basePath!!, path), null)
        root.createChild(BeanSearch.KEY, BeanSearch(true, path))
        root.createChild(SpringBeanData.KEY, component("jdbcOrderStore", ORDER_STORE_IMPL))
        root.createChild(SpringBeanData.KEY, component("legacyReportJob", "com.explyt.demo.jobs.LegacyReportJob"))
        val info = InternalExternalProjectInfo(SYSTEM_ID, path, root).apply {
            lastImportTimestamp = IMPORTED_AT
            lastSuccessfulImportTimestamp = IMPORTED_AT
        }
        ExternalProjectsDataStorage.getInstance(project).update(info)
        ModificationTrackerManager.getInstance(project).invalidateAll()
        assertEquals("Precondition: the root must be loaded", 1, NativeBeanSnapshotReader(project).contexts().size)
    }

    private fun addApplication(): PsiClass {
        myFixture.addClass(
            """
            package com.explyt.demo;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class App {}
            """.trimIndent()
        )
        myFixture.addClass("package com.explyt.demo; public interface OrderStore {}")
        myFixture.addClass(
            """
            package com.explyt.demo;

            import org.springframework.stereotype.Component;

            @Component
            public class JdbcOrderStore implements OrderStore {}
            """.trimIndent()
        )
        return JavaPsiFacade.getInstance(project).findClass(APPLICATION, GlobalSearchScope.projectScope(project))
            ?: error("No PSI for $APPLICATION")
    }

    private fun component(beanName: String, className: String) =
        SpringBeanData(beanName, className, "singleton", null, null, SpringBeanType.COMPONENT, false, true, true)

    private companion object {
        const val APPLICATION = "com.explyt.demo.App"
        const val ORDER_STORE = "com.explyt.demo.OrderStore"
        const val ORDER_STORE_IMPL = "com.explyt.demo.JdbcOrderStore"
        const val IMPORTED_AT = 1_790_000_000_000L
    }
}
