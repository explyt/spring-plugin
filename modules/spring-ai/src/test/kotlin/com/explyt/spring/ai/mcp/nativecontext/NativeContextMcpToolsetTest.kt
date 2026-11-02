/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.nativecontext

import com.explyt.spring.core.externalsystem.model.BeanSearch
import com.explyt.spring.core.externalsystem.model.SpringBeanData
import com.explyt.spring.core.externalsystem.model.SpringBeanType
import com.explyt.spring.core.externalsystem.setting.NativeProjectSettings
import com.explyt.spring.core.externalsystem.setting.NativeSettings
import com.explyt.spring.core.externalsystem.sync.ExternalSystemNativeContextRefresher
import com.explyt.spring.core.externalsystem.sync.NativeContextRefresher
import com.explyt.spring.core.externalsystem.sync.NativeContextSync
import com.explyt.spring.core.externalsystem.sync.RefreshResult
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.runBlockingReleasingWriteIntent
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.internal.InternalExternalProjectInfo
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsDataStorage
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.psi.PsiClass
import kotlinx.coroutines.awaitCancellation
import java.time.Instant

class NativeContextMcpToolsetTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    private val mapper = ObjectMapper()
    private val linkedPaths = mutableListOf<String>()
    private val refreshedPaths = mutableListOf<String>()
    private val sync: NativeContextSync get() = project.service()

    override fun tearDown() {
        try {
            sync.refresher = ExternalSystemNativeContextRefresher
            val settings = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
            linkedPaths.forEach { settings.unlinkExternalProject(it) }
            linkedPaths.clear()
        } finally {
            super.tearDown()
        }
    }

    fun testASyncReportsTheNewSnapshotAndItsBeanCount() = runBlockingReleasingWriteIntent<Unit> {
        val path = link(application())
        sync.refresher = NativeContextRefresher { _, linkedPath ->
            refreshedPaths += linkedPath
            store(linkedPath, "refreshed", listOf("orderStore", "orderService", "clock"), REFRESHED_AT)
            RefreshResult.Completed
        }

        val root = call()

        assertEquals("SYNCED", root["status"].asText())
        assertEquals(listOf(path), refreshedPaths)
        assertEquals(Instant.ofEpochMilli(IMPORTED_AT).toString(), root["previousImportedAt"].asText())
        assertEquals(Instant.ofEpochMilli(REFRESHED_AT).toString(), root["importedAt"].asText())
        assertEquals(3, root["beanCount"].asInt())
        assertEquals(APPLICATION, root["context"]["applicationClassName"].asText())
        assertEquals(RUN_CONFIGURATION, root["context"]["runConfiguration"].asText())
        assertFalse("A linked path is never a path of the machine, got $root",
            root["context"]["linkedPath"].asText().startsWith(project.basePath!! + "/"))
        assertFalse(root.has("stillRunning") || root.has("error") || root.has("choices"))
    }

    fun testAFailedRefreshKeepsThePreviousSnapshotAndNamesTheError() = runBlockingReleasingWriteIntent<Unit> {
        link(application())
        sync.refresher = NativeContextRefresher { _, _ -> RefreshResult.Failed("Build failed") }

        val root = call()

        assertEquals("FAILED", root["status"].asText())
        assertEquals("Build failed", root["error"]["message"].asText())
        assertEquals(Instant.ofEpochMilli(IMPORTED_AT).toString(), root["previousImportedAt"].asText())
        assertFalse(root.has("importedAt") || root.has("beanCount") || root.has("stillRunning"))
    }

    fun testARefreshLongerThanTheTimeoutIsReportedAsTimedOut() = runBlockingReleasingWriteIntent<Unit> {
        link(application())
        sync.refresher = NativeContextRefresher { _, _ -> awaitCancellation() }

        val root = call(timeoutSeconds = 1)

        assertEquals("TIMED_OUT", root["status"].asText())
        assertFalse(root["stillRunning"].asBoolean())
        assertFalse(root.has("importedAt") || root.has("beanCount") || root.has("error"))
    }

    fun testAnApplicationWithoutALinkedContextIsNotLinked() = runBlockingReleasingWriteIntent<Unit> {
        application()
        sync.refresher = recordingRefresher()

        val root = call()

        assertEquals("NOT_LINKED", root["status"].asText())
        assertFalse(root.has("context"))
        assertEquals(emptyList<String>(), refreshedPaths)
    }

    fun testTwoContextsOfOneApplicationRequireAChoice() = runBlockingReleasingWriteIntent<Unit> {
        val application = application()
        link(application, suffix = "#first")
        link(application, suffix = "#second")
        sync.refresher = recordingRefresher()

        val root = call()

        assertEquals("CONTEXT_REQUIRED", root["status"].asText())
        assertEquals(2, root["choices"].size())
        assertTrue(root["choices"].all { it.has("contextId") && it.has("label") })
        assertEquals(emptyList<String>(), refreshedPaths)
    }

    fun testAContextIdFromTheChoicesRefreshesThatContext() = runBlockingReleasingWriteIntent<Unit> {
        val application = application()
        link(application, suffix = "#first")
        link(application, suffix = "#second")
        sync.refresher = recordingRefresher()
        val chosen = call()["choices"][1]["contextId"].asText()

        val root = call(contextId = chosen)

        assertEquals(chosen, root["context"]["contextId"].asText())
        assertEquals(1, refreshedPaths.size)
        assertTrue(refreshedPaths.single().endsWith("#second"))
    }

    fun testADeclinedConfirmationRunsNothing() = runBlockingReleasingWriteIntent<Unit> {
        link(application())
        sync.refresher = recordingRefresher()

        val root = call(confirmed = false)

        assertEquals("DECLINED", root["status"].asText())
        assertEquals(APPLICATION, root["context"]["applicationClassName"].asText())
        assertEquals(Instant.ofEpochMilli(IMPORTED_AT).toString(), root["previousImportedAt"].asText())
        assertEquals(emptyList<String>(), refreshedPaths)
    }

    private fun recordingRefresher() = NativeContextRefresher { _, linkedPath ->
        refreshedPaths += linkedPath
        RefreshResult.Completed
    }

    private suspend fun call(confirmed: Boolean = true, contextId: String? = null, timeoutSeconds: Int = 30): JsonNode {
        val toolset = NativeContextMcpToolset().apply { confirmation = NativeSyncConfirmation { _, _, _ -> confirmed } }
        return mapper.readTree(
            toolset.syncSpringNativeContext(
                projectPath = project.basePath!!,
                applicationClassName = APPLICATION.takeIf { contextId == null },
                contextId = contextId,
                timeoutSeconds = timeoutSeconds
            )
        )
    }

    private fun application(): PsiClass = myFixture.addClass(
        """
        package demo;

        import org.springframework.boot.autoconfigure.SpringBootApplication;

        @SpringBootApplication
        public class DemoApplication {}
        """.trimIndent()
    )

    private fun link(application: PsiClass, suffix: String = ""): String {
        val path = application.navigationElement.containingFile.virtualFile.canonicalPath!! + suffix
        project.getService(NativeSettings::class.java).linkProject(NativeProjectSettings().apply {
            externalProjectPath = path
            qualifiedMainClassName = application.qualifiedName
            runConfigurationName = RUN_CONFIGURATION
        })
        linkedPaths += path
        store(path, application.name!! + suffix, listOf("orderStore"), IMPORTED_AT)
        return path
    }

    private fun store(path: String, name: String, beans: List<String>, importedAt: Long) {
        val root = DataNode(ProjectKeys.PROJECT, ProjectData(SYSTEM_ID, name, project.basePath!!, path), null)
        root.createChild(BeanSearch.KEY, BeanSearch(true, path))
        beans.forEach { beanName ->
            root.createChild(
                SpringBeanData.KEY,
                SpringBeanData(beanName, "demo.DemoApplication", "singleton", null, null, SpringBeanType.OTHER, true, true, false)
            )
        }
        ExternalProjectsDataStorage.getInstance(project).update(
            InternalExternalProjectInfo(SYSTEM_ID, path, root).apply {
                lastImportTimestamp = importedAt
                lastSuccessfulImportTimestamp = importedAt
            }
        )
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }

    private companion object {
        const val APPLICATION = "demo.DemoApplication"
        const val RUN_CONFIGURATION = "DemoApplication"
        const val IMPORTED_AT = 1_767_225_600_000L
        const val REFRESHED_AT = 1_767_229_200_000L
    }
}
