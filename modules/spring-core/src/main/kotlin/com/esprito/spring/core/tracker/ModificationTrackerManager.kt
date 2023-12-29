package com.esprito.spring.core.tracker

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ModificationTrackerManager(project: Project) : Disposable {
    private val uastModelTracker = EspritoModelModificationTracker(project, this)

    companion object {
        fun getInstance(project: Project): ModificationTrackerManager = project.service()
    }

    fun getUastModelAndLibraryTracker() = uastModelTracker

    fun getLibraryTracker() = uastModelTracker.javaLibraryTracker

    override fun dispose() {}
}