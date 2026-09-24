/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.core.service.ProfilesService
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile

/**
 * Builds the bean snapshot of one explicitly chosen model.
 *
 * The context is selected first and read second: merging the loaded roots and filtering afterwards cannot
 * separate a bean that two applications both declare.
 */
@Service(Service.Level.PROJECT)
class BeanSnapshotService(private val project: Project) {

    fun read(
        application: PsiClass,
        source: BeanSourcePreference,
        contextId: String?,
        injectionFile: PsiFile?
    ): ScopedBeanSnapshot {
        val identity = identityOf(application)
        val applicationModule = ModuleUtilCore.findModuleForPsiElement(application)
            ?: throw BeanQueryException(
                BeanQueryProblem(
                    BeanApplicationResolver.APPLICATION_NOT_FOUND,
                    "The application class does not belong to a module."
                )
            )
        val selection = BeanContextSelector.select(
            identity, NativeBeanSnapshotReader(project).contexts(), source, contextId
        )

        val records = when (val context = selection.nativeContext) {
            null -> StaticBeanSnapshotReader(project).read(applicationModule, injectionFile)
            else -> NativeBeanSnapshotReader(project).read(context, applicationModule)
        }

        return ScopedBeanSnapshot(
            application = identity,
            selection = selection,
            modelStamp = modelStampOf(selection),
            records = records,
            // The selector already stated what the chosen source cannot promise; the reader's own limitations are
            // added to that set rather than replacing it.
            limitations = selection.limitations + records.flatMapTo(mutableSetOf()) { it.limitations }
        )
    }

    /**
     * What the answer depended on, so a continuation can tell whether the model moved under it.
     *
     * Deliberately excludes the selected editor and the open file: the same query must fingerprint the same way
     * no matter which tab happens to be focused, or a caller paging through results would be told the model
     * changed when only the UI did.
     */
    private fun modelStampOf(selection: BeanContextSelection): String {
        val trackers = ModificationTrackerManager.getInstance(project)
        val parts = mutableListOf(
            selection.source.name,
            selection.nativeContext?.id ?: "static",
            trackers.getUastModelAndLibraryTracker().modificationCount.toString(),
            trackers.getLibraryTracker().modificationCount.toString()
        )
        // Profiles change which beans the static model reports, so they belong in the stamp; a native snapshot is
        // a recorded context and does not move when the IDE's profile inputs do.
        if (selection.nativeContext == null) {
            parts += ProfilesService.getInstance(project).beanQueryProfileInputs()
        } else {
            parts += "native-root:${selection.nativeContext.linkedPath}"
        }
        return BeanSnapshotIdentity.hash(parts)
    }

    private fun identityOf(application: PsiClass): BeanApplicationIdentity = BeanApplicationIdentity(
        className = application.qualifiedName ?: error("An application without a qualified name cannot be scoped"),
        moduleName = ModuleUtilCore.findModuleForPsiElement(application)?.name ?: "",
        mainSourceKey = application.navigationElement.containingFile?.virtualFile?.path ?: ""
    )

    companion object {
        fun getInstance(project: Project): BeanSnapshotService = project.service()
    }
}
