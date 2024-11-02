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

import com.explyt.spring.core.JavaEeClasses
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.externalsystem.model.SpringBeanData
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.externalsystem.utils.NativeBootUtils
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.SpringCoreUtil.beanPsiType
import com.explyt.spring.core.util.SpringCoreUtil.beanPsiTypeKotlin
import com.explyt.spring.core.util.SpringCoreUtil.filterByBeanPsiType
import com.explyt.spring.core.util.SpringCoreUtil.filterByExactMatch
import com.explyt.spring.core.util.SpringCoreUtil.filterByQualifier
import com.explyt.spring.core.util.SpringCoreUtil.getQualifierAnnotation
import com.explyt.spring.core.util.SpringCoreUtil.matchesWildcardType
import com.explyt.spring.core.util.SpringCoreUtil.possibleMultipleBean
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanName
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.explyt.util.CacheKeyStore
import com.explyt.util.ExplytAnnotationUtil.getLongValue
import com.explyt.util.ExplytPsiUtil.getMetaAnnotation
import com.explyt.util.ExplytPsiUtil.isEqualOrInheritor
import com.explyt.util.ExplytPsiUtil.isGeneric
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.resolvedDeepPsiClass
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.intellij.lang.Language
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.*
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.externalSystem.findAll
import org.jetbrains.kotlin.idea.util.projectStructure.getModule

@Service(Service.Level.PROJECT)
class NativeSearchService(private val project: Project) {

    fun getAllActiveBeans(module: Module): Set<PsiBean> {
        return CachedValuesManager.getManager(project).getCachedValue(module) {
            CachedValueProvider.Result(
                getBeans(module),
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker(),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun getBeans(module: Module): Set<PsiBean> {
        if (!isDependentModule(module)) return emptySet()
        val projectBeans = getProjectBeans(module)
        val libraryBeans = getLibraryBeans()
        return (projectBeans + libraryBeans).toSet()
    }

    fun getAllBeanClasses(module: Module): Set<PsiClass> {
        synchronized(this) {
            return CachedValuesManager.getManager(project).getCachedValue(module) {
                CachedValueProvider.Result(
                    getAllActiveBeans(module).mapTo(mutableSetOf()) { it.psiClass },
                    ModificationTrackerManager.getInstance(project).getExternalSystemTracker(),
                    ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
                )
            }
        }
    }

    fun getBeanPsiClassesAnnotatedByComponent(module: Module): Set<PsiBean> {
        return CachedValuesManager.getManager(project).getCachedValue(module) {
            CachedValueProvider.Result(
                getAllActiveBeans(module).filterTo(mutableSetOf()) { it.psiMember == it.psiClass },
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker(),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun getComponentBeanPsiMethods(module: Module): Set<PsiMethod> {
        return CachedValuesManager.getManager(project).getCachedValue(module) {
            CachedValueProvider.Result(
                getAllActiveBeans(module).mapNotNullTo(mutableSetOf()) { it.psiMember as? PsiMethod },
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker(),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun searchArrayComponentPsiClassesByBeanMethods(module: Module): Set<PsiBean> {
        return getComponentBeanPsiMethods(module)
            .asSequence()
            .filter { it.returnType is PsiArrayType }
            .mapNotNull { method ->
                method.returnType?.resolvedDeepPsiClass
                    ?.let { psiClass -> PsiBean("tmp", psiClass, method.getQualifierAnnotation(), method) }
            }
            .toSet()
    }

    fun getAllBeansClassesWithAncestors(module: Module): Set<PsiClass> {
        return CachedValuesManager.getManager(project).getCachedValue(module) {
            CachedValueProvider.Result(
                getAllActiveBeans(module).flatMapTo(mutableSetOf()) { it.psiClass.supers.asSequence() + it.psiClass },
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker(),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun getAllBeanByNames(module: Module): Map<String, List<PsiBean>> {
        return CachedValuesManager.getManager(project).getCachedValue(module) {
            CachedValueProvider.Result(
                getAllActiveBeans(module).groupBy { it.name },
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker(),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun getSpringMethodBeans(): List<SpringBeanData> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getSpringMethodBeansInner(),
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker()
            )
        }
    }

    fun getActiveProjectsNode(): List<DataNode<ProjectData>> {
        return ProjectDataManager.getInstance().getExternalProjectsData(project, SYSTEM_ID)
            .asSequence()
            .mapNotNull { it.externalProjectStructure }
            .filter { !it.isIgnored }
            .filter { it.children.isNotEmpty() }
            .toList()
    }

    private fun getProjectBeans(module: Module): List<PsiBean> {
        return CachedValuesManager.getManager(project).getCachedValue(module) {
            CachedValueProvider.Result(
                getProjectBeansInner(module),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun getLibraryBeans(): List<PsiBean> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getLibraryBeansInner(),
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker()
            )
        }
    }

    private fun getProjectBeansInner(module: Module): List<PsiBean> {
        val activeProjectsNode = getActiveProjectsNode()
        val beansData = activeProjectsNode
            .flatMapTo(mutableSetOf()) { it.findAll(SpringBeanData.KEY).map { beanData -> beanData.data } }
        val beans = getBeans(beansData, true)
        val nativePsiClasses = beans.map { it.psiClass }.toSet()
        val createdBeans = activeProjectsNode
            .flatMapTo(mutableSetOf()) { searchCreatedSimpleBeans(it, module, nativePsiClasses) }
        return beans + createdBeans
    }

    private fun getLibraryBeansInner(): List<PsiBean> {
        val beansData = getActiveProjectsNode()
            .flatMapTo(mutableSetOf()) { it.findAll(SpringBeanData.KEY).map { beanData -> beanData.data } }
        return getBeans(beansData, false)
    }

    private fun getBeans(beansData: Collection<SpringBeanData>, isProjectBean: Boolean): List<PsiBean> {
        return beansData.asSequence()
            .filter { it.projectBean == isProjectBean }
            .mapNotNull { mapToPsiBean(it) }
            .toList()
    }

    private fun mapToPsiBean(bean: SpringBeanData): PsiBean? {
        return if (bean.methodName == null) simpleMapToPsiBean(bean) else methodMapToPsiBean(bean)
    }

    private fun simpleMapToPsiBean(bean: SpringBeanData): PsiBean? {
        val psiClass = NativeBootUtils.getBeanTypePsiClass(project, bean) ?: return null
        return PsiBean(bean.beanName, psiClass, psiClass.getQualifierAnnotation(), psiClass)
    }

    private fun methodMapToPsiBean(bean: SpringBeanData): PsiBean? {
        val psiClass = NativeBootUtils.getPsiClassLocation(project, bean) ?: return null
        val methodsByName = psiClass.findMethodsByName(bean.methodName, false)
        val method: PsiMethod? = if (methodsByName.size == 1) {
            methodsByName[0]
        } else {
            val psiMethod = methodsByName.find { it.resolveBeanName.contains(bean.beanName) }
            if (psiMethod == null && Registry.`is`("explyt.spring.process.log.all")) {
                LOG.debug("Bean not found: ${bean.beanName}, ${bean.className}, ${bean.methodName}")
                return null
            }
            psiMethod
        }
        return method?.let { toMethodPsiBean(method, bean.beanName) }
    }

    private fun toMethodPsiBean(method: PsiMethod, beanName: String): PsiBean? {
        return method.returnType?.resolvedPsiClass
            ?.let { psiClass -> PsiBean(beanName, psiClass, method.getQualifierAnnotation(), method) }
    }

    private fun searchCreatedSimpleBeans(
        projectNode: DataNode<ProjectData>, module: Module, nativePsiClasses: Set<PsiClass>
    ): List<PsiBean> {
        if (nativePsiClasses.isEmpty()) return emptyList()

        val externalProjectPath = projectNode.data.linkedExternalProjectPath
        val mainModule = getMainModule(externalProjectPath)
        if (!isDependentModule(module, mainModule)) return emptyList()

        val packages = getPackages(externalProjectPath)
        val componentForNative = SpringSearchService.getInstance(project)
            .getBeanPsiClassesAnnotatedByComponentForNative(module)
            .filter { !nativePsiClasses.contains(it.psiClass) && inPackage(packages, it.psiClass) }
        return componentForNative
    }

    private fun isDependentModule(module: Module): Boolean {
        val projectPaths = getActiveProjectsNode().map { it.data.linkedExternalProjectPath }
        if (projectPaths.isEmpty()) return false
        for (projectPath in projectPaths) {
            if (isDependentModule(module, getMainModule(projectPath))) {
                return true
            }
        }
        return false
    }

    private fun isDependentModule(module: Module, mainModule: Module?): Boolean {
        mainModule ?: return false
        if (mainModule == module) return true
        val moduleManager = ModuleManager.getInstance(project)
        val moduleDependent = moduleManager.isModuleDependent(mainModule, module)
                || moduleManager.isModuleDependent(module, mainModule)
        return moduleDependent
    }

    private fun getMainModule(externalProjectPath: String): Module? {
        return LocalFileSystem.getInstance().findFileByPath(externalProjectPath)?.getModule(project)
    }

    private fun getPackages(externalProjectPath: String): Set<String> {
        val key = CacheKeyStore.getInstance(project)
            .getKey<Set<String>>("ExplytSpringBootPackages(${externalProjectPath})")
        return CachedValuesManager.getManager(project).getCachedValue(project, key, {
            CachedValueProvider.Result(
                getPackagesInner(externalProjectPath),
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker()
            )
        }, false)
    }

    private fun getPackagesInner(externalProjectPath: String): Set<String> {
        val beansNode = ProjectDataManager.getInstance()
            .getExternalProjectData(project, SYSTEM_ID, externalProjectPath)
            ?.externalProjectStructure?.findAll(SpringBeanData.KEY) ?: return emptySet()
        return beansNode.asSequence()
            .map { it.data }
            .filter { it.projectBean }
            .mapNotNull { StringUtil.getPackageName(it.className) }
            .toSet()
    }

    private fun inPackage(packages: Set<String>, psiClass: PsiClass): Boolean {
        val packageName = (psiClass.containingFile as? PsiJavaFile)?.packageName ?: return false
        return packages.any { packageName.startsWith(it) }
    }

    private fun getSpringMethodBeansInner(): List<SpringBeanData> {
        return getActiveProjectsNode()
            .flatMap { it.findAll(SpringBeanData.KEY).map { beanData -> beanData.data } }
            .filter { it.methodName != null }
            .toList()
    }

    fun findActiveBeanDeclarations(
        module: Module,
        byBeanName: String,
        language: Language,
        sourcePsiType: PsiType?,
        qualifier: PsiAnnotation?
    ): List<PsiMember> {
        val beanPsiType = if (language == KotlinLanguage.INSTANCE)
            sourcePsiType?.beanPsiTypeKotlin else sourcePsiType?.beanPsiType
        val beanPsiClass = sourcePsiType?.resolveBeanPsiClass
        var isMultipleBean = sourcePsiType?.possibleMultipleBean() ?: false
        val methodsPsiBeans = getComponentBeanPsiMethods(module)
        val beanNameFromQualifier = qualifier?.resolveBeanName()

        val resultByType: List<PsiMember> = if (sourcePsiType != null && beanPsiType != null) {
            val byExactMatch = methodsPsiBeans.filterByExactMatch(sourcePsiType).toSet()
            var byTypeBeanMethods = byExactMatch
            if (isMultipleBean || byExactMatch.isEmpty()) {
                byTypeBeanMethods = methodsPsiBeans.filterByBeanPsiType(beanPsiType).toSet()
            }

            val byTypeComponents = getPsiClassesByComponents(
                module, sourcePsiType, beanPsiType, byTypeBeanMethods.isEmpty()
            ).toSet()

            if (isMultipleBean && byExactMatch.isNotEmpty()
                && byTypeBeanMethods.isEmpty() && byTypeComponents.isEmpty()
            ) {
                // Check if multiple bean has exact type match
                isMultipleBean = false
                byTypeBeanMethods = byExactMatch
            }

            val aResultByTypeAndQualifier: List<PsiMember> = (byTypeBeanMethods + byTypeComponents)
                .filter { it.filterByQualifier(module, qualifier, beanNameFromQualifier) }

            if (aResultByTypeAndQualifier.isEmpty()) {
                return emptyList()
            }

            if (aResultByTypeAndQualifier.size > 1 && !isMultipleBean) {
                val byPrimary = aResultByTypeAndQualifier.filter { it.isMetaAnnotatedBy(SpringCoreClasses.PRIMARY) }
                if (byPrimary.isNotEmpty()) {
                    return byPrimary
                }
                val byPriority = aResultByTypeAndQualifier.asSequence()
                    .filter { it.isMetaAnnotatedBy(JavaEeClasses.PRIORITY.allFqns) }
                    .groupBy {
                        it.getMetaAnnotation(JavaEeClasses.PRIORITY.allFqns)?.getLongValue()?.toInt() ?: Int.MAX_VALUE
                    }
                if (byPriority.isNotEmpty()) {
                    val highestPriority = byPriority.minOf { it.key }
                    return byPriority[highestPriority] ?: emptyList()
                }
            }
            aResultByTypeAndQualifier
        } else {
            val componentPsiBeans = getBeanPsiClassesAnnotatedByComponent(module)
            (methodsPsiBeans + componentPsiBeans.mapTo(mutableSetOf()) { it.psiClass })
                .filter { it.filterByQualifier(module, qualifier, beanNameFromQualifier) }
        }

        if (resultByType.size == 1) {
            return resultByType
        }

        if (qualifier == null) {
            if (beanPsiClass != null && isMultipleBean) {
                return resultByType
            }

            val filteredByBeanName = resultByType
                .filter { byBeanName in it.resolveBeanName(module) }
            return filteredByBeanName.ifEmpty { resultByType }
        }
        val byNameFromQualifierBean = resultByType
            .filter { it.filterByQualifier(module, qualifier, beanNameFromQualifier) }
        return byNameFromQualifierBean
            .filter { byBeanName in it.resolveBeanName(module) }
            .ifEmpty { byNameFromQualifierBean }
    }

    private fun getPsiClassesByComponents(
        module: Module,
        sourcePsiType: PsiType,
        beanPsiType: PsiType,
        byTypeBeanMethodsIsEmpty: Boolean
    ): Sequence<PsiClass> {
        val beanPsiClass = beanPsiType.resolvedPsiClass
        val componentPsiBeans = getBeanPsiClassesAnnotatedByComponent(module)
        val byTypeComponents = componentPsiBeans.asSequence()
            .map { it.psiClass }
            .filter {
                beanPsiClass != null && it.isEqualOrInheritor(beanPsiClass)
                        || beanPsiType is PsiWildcardType && it.matchesWildcardType(beanPsiType)
            }
            .filter { !it.isGeneric(sourcePsiType) || !byTypeBeanMethodsIsEmpty }
        return byTypeComponents
    }

    companion object {
        private val LOG = Logger.getInstance(NativeSearchService::class.java)

        fun getInstance(project: Project): NativeSearchService = project.service()
    }
}