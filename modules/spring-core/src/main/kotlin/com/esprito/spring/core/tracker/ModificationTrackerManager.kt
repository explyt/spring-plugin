package com.esprito.spring.core.tracker

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
    private val uastModelTracker = EspritoModelModificationTracker(project)
    private val uastAnnotationTracker = EspritoAnnotationModificationTracker(project)

    init {
        PsiManager.getInstance(project).addPsiTreeChangeListener(
            MyUastPsiTreeChangeAdapter(project, uastModelTracker, uastAnnotationTracker), this
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

    fun invalidateAll() {
        uastModelTracker.incModificationCount()
        uastAnnotationTracker.incModificationCount()
    }

    fun getLibraryTracker(): ModificationTracker = JavaLibraryModificationTracker.getInstance(project)

    override fun dispose() {}
}