/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.tracker

import com.intellij.java.library.JavaLibraryModificationTracker
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter
import com.intellij.psi.PsiManager

@Service(Service.Level.PROJECT)
class ModificationTrackerManager(val project: Project) : Disposable {
    private val uastModelTracker = ExplytModelModificationTracker(project)
    private val uastAnnotationTracker = ExplytAnnotationModificationTracker(project)
    private val propertyTracker = ExplytPropertyModificationTracker(project)
    private val externalSystemTracker = SpringBootExternalSystemTracker(project)

    init {
        PsiManager.getInstance(project).addPsiTreeChangeListener(
            MyUastPsiTreeChangeAdapter(project, uastModelTracker, uastAnnotationTracker, propertyTracker), this
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

    fun invalidateAll() {
        uastModelTracker.incModificationCount()
        uastAnnotationTracker.incModificationCount()
        externalSystemTracker.incModificationCount()
    }

    fun getLibraryTracker(): ModificationTracker = JavaLibraryModificationTracker.getInstance(project)

    override fun dispose() {}
}