package com.esprito.spring.core.service

import com.esprito.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.intellij.lang.Language
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.*
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

@Service(Service.Level.PROJECT)
class SpringSearchServiceFacade(private val project: Project) {
    private val springSearchService = SpringSearchService.getInstance(project)
    private val nativeSearchService = NativeSearchService.getInstance(project)

    fun getAllBeansClassesConsideringContext(project: Project): SpringSearchService.FoundBeans {
        return springSearchService.getAllBeansClassesConsideringContext(project)
    }

    fun getAllActiveBeanClasses(module: Module): Set<PsiClass> {
        return if (isExternalProjectExist(project)) {
            nativeSearchService.getAllBeanClasses(module)
        } else {
            springSearchService.getAllActiveBeans(module)
        }
    }

    fun getAllActiveBeans(module: Module): Set<PsiBean> {
        return if (isExternalProjectExist(project)) {
            nativeSearchService.getAllActiveBeans(module)
        } else {
            springSearchService.getActiveBeansClasses(module)
        }
    }

    fun getAllActiveBeansLight(module: Module): Set<PsiClass> {
        return if (isExternalProjectExist(project)) {
            nativeSearchService.getAllBeanClasses(module)
        } else {
            springSearchService.getAllActiveBeansLight(module)
        }
    }

    fun getBeanPsiClassesAnnotatedByComponent(module: Module): Set<PsiBean> {
        return if (isExternalProjectExist(project)) {
            nativeSearchService.getBeanPsiClassesAnnotatedByComponent(module)
        } else {
            springSearchService.getBeanPsiClassesAnnotatedByComponent(module)
        }
    }

    fun getAllBeansClassesWithAncestors(module: Module): Set<PsiClass> {
        return if (isExternalProjectExist(project)) {
            nativeSearchService.getAllBeansClassesWithAncestors(module)
        } else {
            springSearchService.getAllBeansClassesWithAncestorsLight(module)
        }
    }

    fun getComponentBeanPsiMethods(module: Module): Set<PsiMethod> {
        return if (isExternalProjectExist(project)) {
            nativeSearchService.getComponentBeanPsiMethods(module)
        } else {
            springSearchService.getComponentBeanPsiMethods(module)
        }
    }

    fun searchArrayComponentPsiClassesByBeanMethods(module: Module): Set<PsiBean> {
        return if (isExternalProjectExist(project)) {
            nativeSearchService.searchArrayComponentPsiClassesByBeanMethods(module)
        } else {
            springSearchService.searchArrayComponentPsiClassesByBeanMethods(module)
        }
    }

    fun getAllBeanByNames(module: Module): Map<String, List<PsiBean>> {
        return if (isExternalProjectExist(project)) {
            nativeSearchService.getAllBeanByNames(module)
        } else {
            springSearchService.getAllBeanByNames(module)
        }
    }

    fun getExcludedBeansClasses(module: Module): Set<PsiBean> {
        return if (isExternalProjectExist(project)) {
            emptySet()
        } else {
            springSearchService.getExcludedBeansClasses(module)
        }
    }

    fun findActiveBeanDeclarations(
        module: Module,
        byBeanName: String,
        language: Language,
        byBeanPsiType: PsiType? = null,
        qualifier: PsiAnnotation? = null
    ): List<PsiMember> {
        return if (isExternalProjectExist(project)) {
            nativeSearchService.findActiveBeanDeclarations(module, byBeanName, language, byBeanPsiType, qualifier)
        } else {
            springSearchService.findActiveBeanDeclarations(module, byBeanName, language, byBeanPsiType, qualifier)
        }
    }

    companion object {
        fun getInstance(project: Project): SpringSearchServiceFacade = project.service()

        fun isExternalProjectExist(project: Project): Boolean {
            if (!Registry.`is`("explyt.spring.native")) return false
            return CachedValuesManager.getManager(project).getCachedValue(project) {
                CachedValueProvider.Result(
                    isExternalProjectExistInternal(project),
                    ModificationTrackerManager.getInstance(project).getExternalSystemTracker()
                )
            }
        }

        private fun isExternalProjectExistInternal(project: Project): Boolean {
            return ProjectDataManager.getInstance()
                .getExternalProjectsData(project, SYSTEM_ID)
                .filter { it.externalProjectStructure?.isIgnored == false }
                .find { it.externalProjectStructure?.children?.isNotEmpty() == true } != null
        }
    }
}