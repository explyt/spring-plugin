/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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