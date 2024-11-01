package com.explyt.spring.aop.service

import com.explyt.spring.core.externalsystem.model.SpringAspectData
import com.explyt.spring.core.service.NativeSearchService
import com.explyt.spring.core.tracker.ModificationTrackerManager
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
            CachedValueProvider.Result(
                getAspectsData().mapTo(mutableSetOf()) { it.aspectQualifiedClassName },
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker()
            )
        }
    }

    fun getPointCutQualifiedClasses(): Set<String> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getAspectsData().mapTo(mutableSetOf()) { it.beanQualifiedClassName },
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker()
            )
        }
    }


    fun getAspectsData(): List<SpringAspectData> {
        return NativeSearchService.getInstance(project).getActiveProjectsNode()
            .flatMap { it.findAll(SpringAspectData.KEY).map { it.data } }
    }

    companion object {
        fun getInstance(project: Project): AspectSearchService = project.service()
    }
}