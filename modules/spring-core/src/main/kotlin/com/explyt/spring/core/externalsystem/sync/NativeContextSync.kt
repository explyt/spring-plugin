/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.sync

import com.explyt.spring.core.externalsystem.model.SpringBeanData
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.service.beans.NativeBeanContext
import com.explyt.spring.core.service.beans.NativeBeanSnapshotReader
import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemProcessingManager
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.base.externalSystem.findAll
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration

sealed interface NativeSyncOutcome {
    data class Synced(val previousImportedAt: Long?, val importedAt: Long, val beanCount: Int) : NativeSyncOutcome
    data class Failed(val previousImportedAt: Long?, val message: String) : NativeSyncOutcome
    data object AlreadyRunning : NativeSyncOutcome
    data class TimedOut(val previousImportedAt: Long?, val stillRunning: Boolean) : NativeSyncOutcome
    data class EndedWithoutResult(val previousImportedAt: Long?) : NativeSyncOutcome
    data object NotTrusted : NativeSyncOutcome
}

sealed interface RefreshResult {
    data object Completed : RefreshResult
    data class Failed(val message: String) : RefreshResult
    data object AlreadyRunning : RefreshResult
}

fun interface NativeContextRefresher {
    suspend fun refresh(project: Project, linkedPath: String): RefreshResult
}

object ExternalSystemNativeContextRefresher : NativeContextRefresher {
    override suspend fun refresh(project: Project, linkedPath: String): RefreshResult = withContext(Dispatchers.IO) {
        val done = CompletableFuture<Void?>()
        ExternalSystemUtil.refreshProject(
            linkedPath,
            ImportSpecBuilder(project, SYSTEM_ID)
                .withCallback(done)
                .withActivateToolWindowOnStart(false)
                .withActivateToolWindowOnFailure(false)
                .dontReportRefreshErrors()
                .dontNavigateToError()
        )
        done.await()
        RefreshResult.Completed
    }
}

@Service(Service.Level.PROJECT)
class NativeContextSync(private val project: Project) {

    @set:TestOnly
    var refresher: NativeContextRefresher = ExternalSystemNativeContextRefresher

    @set:TestOnly
    var trust: (Project) -> Boolean = TrustedProjects::isProjectTrusted

    fun isProjectTrusted(): Boolean = trust(project)

    suspend fun sync(context: NativeBeanContext, timeout: Duration): NativeSyncOutcome {
        val previousImportedAt = context.importedAt
        if (!isProjectTrusted()) return NativeSyncOutcome.NotTrusted
        if (isRunning(context.linkedPath)) return NativeSyncOutcome.AlreadyRunning
        val refreshed = try {
            withTimeoutOrNull(timeout) { refresher.refresh(project, context.linkedPath) }
                ?: return NativeSyncOutcome.TimedOut(previousImportedAt, isRunning(context.linkedPath))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (cancelled: ProcessCanceledException) {
            throw cancelled
        } catch (failure: Exception) {
            return NativeSyncOutcome.Failed(previousImportedAt, failureMessage(failure.cause?.message ?: failure.message.orEmpty()))
        }
        when (refreshed) {
            RefreshResult.AlreadyRunning -> return NativeSyncOutcome.AlreadyRunning
            is RefreshResult.Failed -> return NativeSyncOutcome.Failed(previousImportedAt, failureMessage(refreshed.message))
            RefreshResult.Completed -> Unit
        }
        return readAction {
            val importedAt = NativeBeanSnapshotReader(project).contexts().find { it.id == context.id }?.importedAt
            val root = ProjectDataManager.getInstance().getExternalProjectsData(project, SYSTEM_ID)
                .mapNotNull { it.externalProjectStructure }
                .find { it.data.linkedExternalProjectPath == context.linkedPath }
            if (importedAt == null || importedAt == previousImportedAt || root == null) {
                NativeSyncOutcome.EndedWithoutResult(previousImportedAt)
            } else {
                NativeSyncOutcome.Synced(previousImportedAt, importedAt, root.findAll(SpringBeanData.KEY).size)
            }
        }
    }

    private fun isRunning(linkedPath: String): Boolean = ExternalSystemProcessingManager.getInstance()
        .findTask(ExternalSystemTaskType.RESOLVE_PROJECT, SYSTEM_ID, linkedPath) != null

    private fun failureMessage(message: String): String {
        val basePath = project.basePath ?: return message.take(500)
        return message.replace("$basePath/", "").replace(basePath, ".").take(500)
    }
}
