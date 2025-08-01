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

import com.explyt.spring.core.JavaEeClasses.PRIORITY
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.externalsystem.model.BeanSearch
import com.explyt.spring.core.externalsystem.model.SpringBeanData
import com.explyt.spring.core.externalsystem.utils.Constants
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.externalsystem.utils.NativeBootUtils
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.SpringCoreUtil
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
import com.explyt.util.ExplytAnnotationUtil.getLongValue
import com.explyt.util.ExplytKotlinUtil.filterToList
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
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.*
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.externalSystem.findAll
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class NativeSearchService(private val project: Project) {

    fun getAllActiveBeans(): Set<PsiBean> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getBeans(),
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker(),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun getBeans(): Set<PsiBean> {
        val projectBeans = getProjectBeans()
        val libraryBeans = getLibraryBeans()
        return (projectBeans + libraryBeans).toSet()
    }

    fun getAllBeanClasses(): Set<PsiClass> {
        synchronized(this) {
            return CachedValuesManager.getManager(project).getCachedValue(project) {
                CachedValueProvider.Result(
                    SpringSearchUtils.getPsiClasses(getAllActiveBeans()),
                    ModificationTrackerManager.getInstance(project).getExternalSystemTracker(),
                    ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
                )
            }
        }
    }

    fun getBeanPsiClassesAnnotatedByComponent(): Set<PsiBean> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getAllActiveBeans().filterTo(mutableSetOf()) { it.psiMember == it.psiClass },
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker(),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun getBeanPsiMethods(): Set<PsiMethod> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getAllActiveBeans().mapNotNullTo(mutableSetOf()) { it.psiMember as? PsiMethod },
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker(),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun searchArrayPsiClassesByBeanMethods(): Set<PsiBean> {
        return getBeanPsiMethods()
            .asSequence()
            .filter { it.returnType is PsiArrayType }
            .mapNotNull { method ->
                method.returnType?.resolvedDeepPsiClass
                    ?.let { psiClass -> PsiBean("tmp", psiClass, method.getQualifierAnnotation(), method) }
            }
            .toSet()
    }

    fun getAllBeanByNames(): Map<String, List<PsiBean>> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getAllActiveBeans().groupBy { it.name },
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker(),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun getSpringMethodBeans(): List<SpringBeanData> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getSpringMethodBeansInner(),
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker(),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun getActiveProjectsNode(): List<DataNode<ProjectData>> {
        if (SpringCoreUtil.isExplytDebug(project)) {
            return ProjectDataManager.getInstance().getExternalProjectsData(project, SYSTEM_ID)
                .asSequence()
                .mapNotNull { it.externalProjectStructure }
                .filterToList { it.data.externalName == Constants.DEBUG_SESSION_NAME }
        }
        return getLoadedProjects(project)
            .filterToList { isActiveDiPredicate(it) }
    }

    fun getAllProjectsNode(): List<DataNode<ProjectData>> {
        if (SpringCoreUtil.isExplytDebug(project)) {
            return ProjectDataManager.getInstance().getExternalProjectsData(project, SYSTEM_ID)
                .asSequence()
                .mapNotNull { it.externalProjectStructure }
                .filterToList { it.data.externalName == Constants.DEBUG_SESSION_NAME }
        }
        return getLoadedProjects(project).toList()
    }

    fun getLibraryBeans(): List<PsiBean> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getLibraryBeansInner(),
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker()
            )
        }
    }

    /**
     * For library source code analyze only.
     * */
    fun getAllProjectNodesLibraryBeans(): List<PsiBean> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getAllLibraryBeansInner(),
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker()
            )
        }
    }

    fun getProjectBeans(): List<PsiBean> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getProjectBeansInner(),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun getProjectBeansInner(): List<PsiBean> {
        val beansData = getActiveProjectsNode()
            .flatMapTo(mutableSetOf()) { it.findAll(SpringBeanData.KEY).map { beanData -> beanData.data } }
        return getBeans(beansData, true)
    }

    private fun getLibraryBeansInner(): List<PsiBean> {
        val beansData = getActiveProjectsNode()
            .flatMapTo(mutableSetOf()) { it.findAll(SpringBeanData.KEY).map { beanData -> beanData.data } }
        return getBeans(beansData, false)
    }

    private fun getAllLibraryBeansInner(): List<PsiBean> {
        val beansData = getAllProjectsNode()
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

    private fun getSpringMethodBeansInner(): List<SpringBeanData> {
        return getActiveProjectsNode()
            .flatMap { it.findAll(SpringBeanData.KEY).map { beanData -> beanData.data } }
            .filter { it.methodName != null }
            .toList()
    }

    private fun getPsiClassesByComponents(
        allPsiBeans: Collection<PsiBean>,
        sourcePsiType: PsiType,
        beanPsiType: PsiType,
        byTypeBeanMethodsIsEmpty: Boolean
    ): Sequence<PsiClass> {
        val beanPsiClass = beanPsiType.resolvedPsiClass
        val componentPsiBeans = allPsiBeans.filter { it.psiMember == it.psiClass }
        val byTypeComponents = componentPsiBeans.asSequence()
            .map { it.psiClass }
            .filter {
                beanPsiClass != null && it.isEqualOrInheritor(beanPsiClass)
                        || beanPsiType is PsiWildcardType && it.matchesWildcardType(beanPsiType)
            }
            .filter { !it.isGeneric(sourcePsiType) || !byTypeBeanMethodsIsEmpty }
        return byTypeComponents
    }

    fun findActiveBeanDeclarations(
        allPsiBeans: Collection<PsiBean>,
        byBeanName: String,
        language: Language,
        sourcePsiType: PsiType?,
        qualifier: PsiAnnotation?
    ): List<PsiMember> {
        val beanPsiType = if (language == KotlinLanguage.INSTANCE)
            sourcePsiType?.beanPsiTypeKotlin else sourcePsiType?.beanPsiType
        val beanPsiClass = sourcePsiType?.resolveBeanPsiClass
        val isMultipleBean = AtomicBoolean(sourcePsiType?.possibleMultipleBean() ?: false)
        val beanNameFromQualifier = qualifier?.resolveBeanName()

        val resultByType: List<PsiBean> = if (sourcePsiType != null && beanPsiType != null) {
            findActiveBeanDeclarations(
                allPsiBeans, isMultipleBean, sourcePsiType, beanPsiType, qualifier, beanNameFromQualifier
            )
        } else {
            allPsiBeans.filter { it.filterByQualifier(qualifier, beanNameFromQualifier) }
        }

        if (resultByType.size == 1) {
            return resultByType.map { it.psiMember }
        }

        if (qualifier == null) {
            if (beanPsiClass != null && isMultipleBean.get()) {
                return resultByType.map { it.psiMember }
            }

            val filteredByBeanName = resultByType.filter { byBeanName == it.name }
            return filteredByBeanName.map { it.psiMember }.ifEmpty { resultByType.map { it.psiMember } }
        }
        val byNameFromQualifierBean = resultByType
            .filter { it.filterByQualifier(qualifier, beanNameFromQualifier) }
            .map { it.psiMember }
        return byNameFromQualifierBean
            .filter { byBeanName == it.name }
            .ifEmpty { byNameFromQualifierBean }
    }

    private fun findActiveBeanDeclarations(
        allPsiBeans: Collection<PsiBean>,
        atomicIsMultipleBean: AtomicBoolean,
        sourcePsiType: PsiType,
        beanPsiType: PsiType,
        qualifier: PsiAnnotation?,
        beanNameFromQualifier: String?
    ): List<PsiBean> {
        val methodsPsiBeans = allPsiBeans.mapNotNullTo(mutableSetOf()) { it.psiMember as? PsiMethod }
        val byExactMatch = methodsPsiBeans.filterByExactMatch(sourcePsiType).toSet()
        var byTypeBeanMethods = byExactMatch
        if (atomicIsMultipleBean.get() || byExactMatch.isEmpty()) {
            byTypeBeanMethods = methodsPsiBeans.filterByBeanPsiType(beanPsiType).toSet()
        }

        val byTypeComponents = getPsiClassesByComponents(
            allPsiBeans, sourcePsiType, beanPsiType, byTypeBeanMethods.isEmpty()
        ).toSet()

        if (atomicIsMultipleBean.get() && byExactMatch.isNotEmpty()
            && byTypeBeanMethods.isEmpty() && byTypeComponents.isEmpty()
        ) {
            // Check if multiple bean has exact type match
            atomicIsMultipleBean.set(false)
            byTypeBeanMethods = byExactMatch
        }

        val psiMembersSet = mutableSetOf<PsiMember>()
        psiMembersSet += byTypeBeanMethods
        psiMembersSet += byTypeComponents

        val foundPsiBeans = allPsiBeans.filter { it.psiMember in psiMembersSet }

        val aResultByTypeAndQualifier: List<PsiBean> = foundPsiBeans
            .filter { it.filterByQualifier(qualifier, beanNameFromQualifier) }

        if (aResultByTypeAndQualifier.isEmpty()) {
            return emptyList()
        }

        if (aResultByTypeAndQualifier.size > 1 && !atomicIsMultipleBean.get()) {
            val byPrimary =
                aResultByTypeAndQualifier.filter { it.psiMember.isMetaAnnotatedBy(SpringCoreClasses.PRIMARY) }
            if (byPrimary.isNotEmpty()) {
                return byPrimary
            }
            val byPriority = aResultByTypeAndQualifier.asSequence()
                .filter { it.psiMember.isMetaAnnotatedBy(PRIORITY.allFqns) }
                .groupBy {
                    it.psiMember.getMetaAnnotation(PRIORITY.allFqns)?.getLongValue()?.toInt() ?: Int.MAX_VALUE
                }
            if (byPriority.isNotEmpty()) {
                val highestPriority = byPriority.minOf { it.key }
                return byPriority[highestPriority] ?: emptyList()
            }
        }
        return aResultByTypeAndQualifier
    }

    companion object {
        private val LOG = Logger.getInstance(NativeSearchService::class.java)

        fun getInstance(project: Project): NativeSearchService = project.service()

        fun getLoadedProjects(project: Project): Sequence<DataNode<ProjectData>> =
            ProjectDataManager.getInstance().getExternalProjectsData(project, SYSTEM_ID)
                .asSequence()
                .mapNotNull { it.externalProjectStructure }
                .filter { it.data.externalName != Constants.DEBUG_SESSION_NAME }
                .filter { !it.isIgnored }

        fun isActiveDiPredicate(node: DataNode<ProjectData>): Boolean =
            node.children.any { (it.data as? BeanSearch)?.enabled == true }
    }
}