/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.tracker

import com.explyt.spring.core.externalsystem.action.AnnotationTrackerHolderService
import com.intellij.java.library.JavaLibraryModificationTracker
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter
import com.intellij.psi.PsiManager

@Service(Service.Level.PROJECT)
class ModificationTrackerManager(val project: Project) : Disposable {
    private val uastModelTracker = ExplytModelModificationTracker(project)
    private val uastAnnotationTracker = ExplytAnnotationModificationTracker(project)
    private val propertyTracker = ExplytPropertyModificationTracker(project)
    private val externalSystemTracker = SpringBootExternalSystemTracker(project)
    private val refreshFloatingAnnotationTracker = SimpleModificationTracker()

    init {
        PsiManager.getInstance(project).addPsiTreeChangeListener(
            MyUastPsiTreeChangeAdapter(
                project, uastModelTracker, uastAnnotationTracker, propertyTracker, refreshFloatingAnnotationTracker
            ), this
        )
        project.messageBus.connect(this)
            .subscribe(
                VirtualFileManager.VFS_CHANGES,
                BulkVirtualFileListenerAdapter(SpringVirtualFileListener(project, uastModelTracker))
            )
    }

    companion object {
        fun getInstance(project: Project): ModificationTrackerManager = project.service()
    }


    fun getUastModelAndLibraryTracker() = uastModelTracker

    fun getUastAnnotationAndLibraryTracker() = uastAnnotationTracker

    fun getPropertyTracker() = propertyTracker

    fun getExternalSystemTracker() = externalSystemTracker

    fun getRefreshFloatingAnnotationTracker() = refreshFloatingAnnotationTracker

    fun invalidateAll() {
        uastModelTracker.incModificationCount()
        uastAnnotationTracker.incModificationCount()
        externalSystemTracker.incModificationCount()
        refreshFloatingAnnotationTracker.incModificationCount()

        AnnotationTrackerHolderService.getInstance(project).updateAnnotationTrackerHolder()
    }

    fun getLibraryTracker(): ModificationTracker = JavaLibraryModificationTracker.getInstance(project)

    override fun dispose() {}
}