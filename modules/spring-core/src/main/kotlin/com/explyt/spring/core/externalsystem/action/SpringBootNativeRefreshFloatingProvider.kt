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

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.toolbar.floating.AbstractFloatingToolbarProvider
import com.intellij.openapi.editor.toolbar.floating.isInsideMainEditor
import com.intellij.openapi.project.Project
import java.util.concurrent.atomic.AtomicLong

class SpringBootNativeRefreshFloatingProvider
    : AbstractFloatingToolbarProvider("Explyt.SpringBootProjectRefreshActionGroup") {

    override val autoHideable = true

    override fun isApplicable(dataContext: DataContext): Boolean {
        return isInsideMainEditor(dataContext)
    }
}

@Service(Service.Level.PROJECT)
class AnnotationTrackerHolderService(val project: Project) {

    private val annotationTrackerHolder = AtomicLong(
        ModificationTrackerManager.getInstance(project).getRefreshFloatingAnnotationTracker().modificationCount
    )

    fun updateAnnotationTrackerHolder() {
        annotationTrackerHolder.set(
            ModificationTrackerManager.getInstance(project).getRefreshFloatingAnnotationTracker().modificationCount
        )
    }

    fun needUpdate(): Boolean {
        val settingsState = SpringToolRunConfigurationsSettingsState.getInstance()
        if (!settingsState.isShowFloatingRefreshAction) return false

        val modificationTracker = ModificationTrackerManager.getInstance(project).getRefreshFloatingAnnotationTracker()
        val currentTracker = annotationTrackerHolder.get()
        val modificationCount = modificationTracker.modificationCount
        return currentTracker < modificationCount
    }

    companion object {
        fun getInstance(project: Project): AnnotationTrackerHolderService = project.service()
    }
}