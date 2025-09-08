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

package com.explyt.spring.core.service

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.NativeSearchService.Companion.getLoadedProjects
import com.explyt.spring.core.service.NativeSearchService.Companion.isActiveDiPredicate
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.spring.core.util.SpringCoreUtil.getQualifierAnnotation
import com.explyt.spring.core.util.SpringCoreUtil.isCandidate
import com.explyt.util.ExplytPsiUtil.allSupers
import com.explyt.util.ExplytPsiUtil.isAnnotatedBy
import com.intellij.lang.Language
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.*
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.uast.*

@Service(Service.Level.PROJECT)
class SpringSearchServiceFacade(private val project: Project) {
    private val springSearchService = SpringSearchService.getInstance(project)
    private val nativeSearchService = NativeSearchService.getInstance(project)

    fun getAllBeansClassesConsideringContext(project: Project): SpringSearchService.FoundBeans {
        return springSearchService.getAllBeansClassesConsideringContext(project)
    }


    fun getAllActiveBeans(module: Module, isNative: Boolean = false): Set<PsiBean> {
        return if (isNative || isExternalProjectExist(project)) {
            nativeSearchService.getAllActiveBeans()
        } else {
            springSearchService.getActiveBeansClasses(module)
        }
    }

    fun getAllActiveBeansLight(module: Module): Set<PsiClass> {
        return if (isExternalProjectExist(project)) {
            nativeSearchService.getAllBeanClasses()
        } else {
            springSearchService.getAllActiveBeansLight(module)
        }
    }

    fun getComponentBeanPsiMethods(module: Module): Set<PsiMethod> {
        return if (isExternalProjectExist(project)) {
            nativeSearchService.getBeanPsiMethods()
        } else {
            springSearchService.getComponentBeanPsiMethods(module)
        }
    }

    fun searchArrayComponentPsiClassesByBeanMethods(module: Module): Set<PsiBean> {
        return if (isExternalProjectExist(project)) {
            nativeSearchService.searchArrayPsiClassesByBeanMethods()
        } else {
            springSearchService.searchArrayComponentPsiClassesByBeanMethods(module)
        }
    }

    fun getAllBeanByNames(module: Module): Map<String, List<PsiBean>> {
        return if (isExternalProjectExist(project)) {
            nativeSearchService.getAllBeanByNames()
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
            val beans = nativeSearchService.getAllActiveBeans()
            nativeSearchService.findActiveBeanDeclarations(beans, byBeanName, language, byBeanPsiType, qualifier)
        } else {
            springSearchService.findActiveBeanDeclarations(module, byBeanName, language, byBeanPsiType, qualifier)
        }
    }

    fun findFieldsAndMethodsWithAutowired(
        uClass: UClass?, uMethod: UMethod?, module: Module, isNative: Boolean = false
    ): Collection<PsiElement> {
        StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_BEAN_USAGE)
        val isArrayType = uMethod?.returnType is PsiArrayType
        val uElement = uClass ?: uMethod ?: throw RuntimeException("No uElement")
        val targetType = if (uElement is UMethod) uElement.returnType else null

        val targetClass = SpringSearchUtils.getBeanClass(uElement, isArrayType) ?: return emptyList()
        val targetClasses = targetClass.allSupers()

        val allAutowiredAnnotations = SpringSearchUtils.getAutowiredFieldAnnotations(module)
        val allAutowiredAnnotationsNames = allAutowiredAnnotations.mapNotNull { it.qualifiedName }

        val allBeans = getAllActiveBeans(module, isNative)

        val allFieldsWithAutowired = allBeans.asSequence()
            .mapNotNull { bean -> bean.psiClass.toUElementOfType<UClass>()?.fields }
            .flatMap { field ->
                field.asSequence()
                    .filter { it.isAnnotatedBy(allAutowiredAnnotationsNames) }
                    .filter { it.isCandidate(targetType, targetClasses, targetClass) }
                    .mapNotNull { it.navigationElement.toUElement() as? UVariable }
            }.toSet()


        val allParametersWithAutowired = mutableSetOf<UVariable>()
        allBeans.forEach { bean ->
            val methods = bean.psiClass.toUElementOfType<UClass>()?.methods ?: return@forEach
            allParametersWithAutowired.addAll(
                methods.asSequence()
                    .filter {
                        it.isAnnotatedBy(allAutowiredAnnotationsNames)
                                || it.isAnnotatedBy(SpringCoreClasses.BEAN)
                                || it.isConstructor
                                && bean in nativeSearchService.getBeanPsiClassesAnnotatedByComponent()
                    }
                    .flatMap { it.parameterList.parameters.asSequence() }
                    .filter { it.isCandidate(targetType, targetClass, targetClasses) }
                    .map { it.navigationElement.toUElement() as? UVariable }
                    .filterNotNull().toSet())
        }

        val allByType = allFieldsWithAutowired + allParametersWithAutowired
        val filteredByName = allByType.filter {
            val beanName = it.name ?: return@filter true
            val beanPsiType = it.type
            val allActiveBeans = nativeSearchService.getAllActiveBeans()
            val resolvedBeanTargets = nativeSearchService.findActiveBeanDeclarations(
                allActiveBeans, beanName, it.language, beanPsiType, it.getQualifierAnnotation()
            )
            return@filter uElement.javaPsi in resolvedBeanTargets
        }

        return filteredByName.ifEmpty {
            allByType
        }
    }

    companion object {
        fun getInstance(project: Project): SpringSearchServiceFacade = project.service()

        fun isExternalProjectExist(project: Project): Boolean {
            if (SpringCoreUtil.isExplytDebug(project)) return true

            if (isUnitTest(project)) return false
            return CachedValuesManager.getManager(project).getCachedValue(project) {
                CachedValueProvider.Result(
                    isExternalProjectExistInternal(project),
                    ModificationTrackerManager.getInstance(project).getExternalSystemTracker()
                )
            }
        }

        fun isUnitTest(project: Project): Boolean {
            val file = FileEditorManager.getInstance(project).selectedEditor?.file ?: return false
            val fileIndex = ProjectRootManager.getInstance(project).fileIndex
            return fileIndex.isInTestSourceContent(file)
        }

        private fun isExternalProjectExistInternal(project: Project): Boolean {
            return getLoadedProjects(project).any { isActiveDiPredicate(it) }
        }
    }
}