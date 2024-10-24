package com.esprito.spring.aop.service

import com.esprito.spring.core.externalsystem.model.SpringAspectData
import com.esprito.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.esprito.spring.core.runconfiguration.SpringBootRunConfiguration
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.intellij.execution.RunManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.idea.base.externalSystem.findAll


@Service(Service.Level.PROJECT)
class AspectSearchService(private val project: Project) {

    fun getAspectQualifiedClasses(): Set<String> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            val aspectNodes = getExternalProject(project)?.findAll(SpringAspectData.KEY) ?: emptyList()
            CachedValueProvider.Result(
                aspectNodes.mapTo(mutableSetOf()) { it.data.aspectQualifiedClassName },
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker()
            )
        }
    }

    fun getPointCutQualifiedClasses(): Set<String> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            val aspectNodes = getExternalProject(project)?.findAll(SpringAspectData.KEY) ?: emptyList()
            CachedValueProvider.Result(
                aspectNodes.mapTo(mutableSetOf()) { it.data.beanQualifiedClassName },
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker()
            )
        }
    }

    fun getAspectsData(): List<SpringAspectData> {
        return getExternalProject(project)?.findAll(SpringAspectData.KEY)?.map { it.data } ?: emptyList()
    }

    companion object {
        fun getInstance(project: Project): AspectSearchService = project.service()

        //todo to common code
        fun getExternalProject(project: Project): DataNode<ProjectData>? {
            val externalProjectPath = getExternalProjectPath(project) ?: return null
            return ProjectDataManager.getInstance()
                .getExternalProjectData(project, SYSTEM_ID, externalProjectPath)
                ?.externalProjectStructure
        }

        private fun getExternalProjectPath(project: Project): String? {
            return (RunManager.getInstance(project).selectedConfiguration
                ?.configuration as? SpringBootRunConfiguration)?.mainClass?.containingFile?.virtualFile?.canonicalPath
        }
    }
}

data class AspectClasses(private val aspectMap: Map<String, Set<String>>) {

    fun isAspect(psiClass: PsiClass) = aspectMap.contains(psiClass.qualifiedName)

    fun isAspectMethod(psiClass: PsiClass, psiMethod: PsiMethod) = aspectMap[psiClass.qualifiedName]
        ?.contains(psiMethod.name) == true
}