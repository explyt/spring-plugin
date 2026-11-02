/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.sync

import com.explyt.spring.core.externalsystem.model.BeanSearch
import com.explyt.spring.core.externalsystem.model.SpringBeanData
import com.explyt.spring.core.externalsystem.model.SpringBeanType
import com.explyt.spring.core.externalsystem.setting.NativeProjectSettings
import com.explyt.spring.core.externalsystem.setting.NativeSettings
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.service.beans.NativeBeanContext
import com.explyt.spring.core.service.beans.NativeBeanSnapshotReader
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.runBlockingReleasingWriteIntent
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.internal.InternalExternalProjectInfo
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsDataStorage
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class NativeContextSyncTest : ExplytJavaLightTestCase() {
    override val libraries: Array<TestLibrary> = emptyArray()
    private lateinit var linkedPath: String

    override fun setUp() {
        super.setUp()
        val application = myFixture.addClass("package com.explyt.demo; public class App {}")
        linkedPath = application.navigationElement.containingFile.virtualFile.canonicalPath!!
        project.getService(NativeSettings::class.java).linkProject(NativeProjectSettings().apply {
            externalProjectPath = linkedPath
            qualifiedMainClassName = application.qualifiedName
        })
        installRoot(IMPORTED_AT, 1)
    }

    override fun tearDown() {
        try {
            ExternalSystemApiUtil.getSettings(project, SYSTEM_ID).unlinkExternalProject(linkedPath)
        } finally {
            super.tearDown()
        }
    }

    fun testSyncedReadsUpdatedImportTimeAndBeans() = syncTest {
        val context = context()
        val sync = service {
            installRoot(IMPORTED_AT + 1000, 3)
            RefreshResult.Completed
        }
        assertEquals(NativeSyncOutcome.Synced(IMPORTED_AT, IMPORTED_AT + 1000, 3), sync.sync(context, 2.seconds))
    }

    fun testFailedRelativizesProjectPathsBeforeTruncating() = syncTest {
        val sync = service { RefreshResult.Failed("Cannot import ${project.basePath}/src/App.java: " + "x".repeat(700)) }
        val result = sync.sync(context(), 2.seconds) as NativeSyncOutcome.Failed
        assertEquals(IMPORTED_AT, result.previousImportedAt)
        assertEquals(500, result.message.length)
        assertTrue(result.message.startsWith("Cannot import src/App.java: "))
        assertFalse(result.message.contains(project.basePath!!))
    }

    fun testTimedOutCancelsWaitAndReportsNoRunningTask() = syncTest {
        var cancelled = false
        val sync = service {
            try { awaitCancellation() } finally { cancelled = true }
        }
        assertEquals(NativeSyncOutcome.TimedOut(IMPORTED_AT, false), sync.sync(context(), 100.milliseconds))
        assertTrue(cancelled)
    }

    fun testAlreadyRunningFromRefreshRace() = syncTest {
        assertEquals(NativeSyncOutcome.AlreadyRunning, service { RefreshResult.AlreadyRunning }.sync(context(), 2.seconds))
    }

    fun testEndedWithoutResultWhenImportTimeIsUnchanged() = syncTest {
        assertEquals(NativeSyncOutcome.EndedWithoutResult(IMPORTED_AT), service { RefreshResult.Completed }.sync(context(), 2.seconds))
    }

    fun testCallerCancellationPropagates() = syncTest {
        try {
            service { throw CancellationException("caller cancelled") }.sync(context(), 2.seconds)
            fail("Cancellation must propagate")
        } catch (expected: CancellationException) {
            assertEquals("caller cancelled", expected.message)
        }
    }

    fun testUntrustedProjectIsNeverRefreshed() = syncTest {
        val sync = service {
            fail("An untrusted project must not be refreshed")
            RefreshResult.Completed
        }
        sync.trust = { false }
        assertEquals(NativeSyncOutcome.NotTrusted, sync.sync(context(), 2.seconds))
    }

    private fun service(refresh: suspend () -> RefreshResult) = NativeContextSync(project).apply {
        refresher = NativeContextRefresher { actualProject, actualPath ->
            assertSame(project, actualProject)
            assertEquals(linkedPath, actualPath)
            refresh()
        }
    }

    private fun context(): NativeBeanContext = NativeBeanSnapshotReader(project).contexts().single { it.linkedPath == linkedPath }

    private fun installRoot(importedAt: Long, beanCount: Int) {
        val root = DataNode(ProjectKeys.PROJECT, ProjectData(SYSTEM_ID, "demo", project.basePath!!, linkedPath), null)
        root.createChild(BeanSearch.KEY, BeanSearch(true, linkedPath))
        repeat(beanCount) {
            root.createChild(SpringBeanData.KEY,
                SpringBeanData("bean$it", "com.explyt.demo.App", "singleton", null, null, SpringBeanType.COMPONENT, false, true, true))
        }
        val info = InternalExternalProjectInfo(SYSTEM_ID, linkedPath, root).apply {
            lastImportTimestamp = importedAt
            lastSuccessfulImportTimestamp = importedAt
        }
        ExternalProjectsDataStorage.getInstance(project).update(info)
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }

    private fun syncTest(action: suspend () -> Unit) = runBlockingReleasingWriteIntent { action() }

    private companion object { const val IMPORTED_AT = 1_790_000_000_000L }
}
