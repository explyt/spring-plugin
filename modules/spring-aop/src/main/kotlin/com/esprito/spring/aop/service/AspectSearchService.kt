package com.esprito.spring.aop.service

import com.esprito.spring.core.externalsystem.model.SpringAspectData
import com.esprito.spring.core.service.NativeSearchService
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.idea.base.externalSystem.findAll


@Service(Service.Level.PROJECT)
class AspectSearchService(private val project: Project) {

    fun getAspectQualifiedClasses(): Set<String> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            val aspectNodes = NativeSearchService.getInstance(project).getExternalProjectNode()
                ?.findAll(SpringAspectData.KEY) ?: emptyList()
            CachedValueProvider.Result(
                aspectNodes.mapTo(mutableSetOf()) { it.data.aspectQualifiedClassName },
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker()
            )
        }
    }

    fun getPointCutQualifiedClasses(): Set<String> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            val aspectNodes = NativeSearchService.getInstance(project).getExternalProjectNode()
                ?.findAll(SpringAspectData.KEY) ?: emptyList()
            CachedValueProvider.Result(
                aspectNodes.mapTo(mutableSetOf()) { it.data.beanQualifiedClassName },
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker()
            )
        }
    }

    fun getAspectsData(): List<SpringAspectData> {
        return NativeSearchService.getInstance(project).getExternalProjectNode()
            ?.findAll(SpringAspectData.KEY)?.map { it.data } ?: emptyList()
    }

    companion object {
        fun getInstance(project: Project): AspectSearchService = project.service()
    }
}