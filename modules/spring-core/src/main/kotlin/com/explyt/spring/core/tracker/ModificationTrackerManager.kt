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