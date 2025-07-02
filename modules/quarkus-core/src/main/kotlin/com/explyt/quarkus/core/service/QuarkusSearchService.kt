/*
 * Copyright © 2025 Explyt Ltd
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

package com.explyt.quarkus.core.service

import com.explyt.base.LibraryClassCache
import com.explyt.quarkus.core.QuarkusCoreClasses
import com.explyt.quarkus.core.QuarkusUtil
import com.explyt.spring.core.JavaEeClasses.PRIORITY
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.SpringCoreUtil.filterByQualifier
import com.explyt.spring.core.util.SpringCoreUtil.isEqualOrInheritorType
import com.explyt.spring.core.util.SpringCoreUtil.isExactMatch
import com.explyt.spring.core.util.SpringCoreUtil.matchesWildcardType
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanName
import com.explyt.util.ExplytAnnotationUtil.getLongValue
import com.explyt.util.ExplytPsiUtil.getMetaAnnotation
import com.explyt.util.ExplytPsiUtil.isEqualOrInheritor
import com.explyt.util.ExplytPsiUtil.isGeneric
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isNonPrivate
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.explyt.util.ModuleUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.codeInspection.isInheritorOf
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.uast.UVariable
import java.util.concurrent.atomic.AtomicBoolean

//inspections @All to list, Decorate for interface only, one constructor


@Service(Service.Level.PROJECT)
class QuarkusSearchService(private val project: Project) {

    fun allBeanSequence(module: Module): Sequence<PsiBean> {
        synchronized(this) {
            val projectBeans = searchBeansByProject()
            val libraryBeans = searchBeansByLibraryScopeCached(module)
            return projectBeans.asSequence() + libraryBeans.asSequence()
        }
    }

    fun allBeanSequence(): Sequence<PsiBean> {
        synchronized(this) {
            val projectBeans = searchBeansByProject()
            val libraryBeans = getLibraryBeans()
            return projectBeans.asSequence() + libraryBeans.asSequence()
        }
    }

    fun getLibraryBeans(): List<PsiBean> {
        synchronized(this) {
            return CachedValuesManager.getManager(project).getCachedValue(project) {
                val searchScope = ModuleUtil.getOnlyLibrarySearchScope(project)
                val beansByAnnotations = searchBeansByAnnotations(searchScope)
                val libraryBeans = beansByAnnotations.flatMap { findProducesBeans(it) + it }
                CachedValueProvider.Result(
                    libraryBeans, ModificationTrackerManager.getInstance(project).getLibraryTracker()
                )
            }
        }
    }

    private fun searchBeansByProject(): List<PsiBean> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            val searchScope = project.projectScope()
            val beansByAnnotations = searchBeansByAnnotations(searchScope)
            val libraryBeans = beansByAnnotations.flatMap { findProducesBeans(it) + it }
            CachedValueProvider.Result(
                libraryBeans, ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun searchBeansByLibraryScopeCached(module: Module): List<PsiBean> {
        return CachedValuesManager.getManager(project).getCachedValue(module) {
            val searchScope = ModuleUtil.getOnlyLibrarySearchScope(module)
            val beansByAnnotations = searchBeansByAnnotations(searchScope)
            val libraryBeans = beansByAnnotations.flatMap { findProducesBeans(it) + it }
            CachedValueProvider.Result(
                libraryBeans, ModificationTrackerManager.getInstance(project).getLibraryTracker()
            )
        }
    }

    private fun searchBeansByAnnotations(scope: SearchScope): List<PsiBean> {
        val annotationPsiClasses = getBeanAnnotations()
        return annotationPsiClasses.asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, scope) }
            .filter { QuarkusUtil.isBeanCandidateClass(it) } // todo check it
            .map { PsiBean(it.qualifiedName!!, it, null, it) }
            .toList()
    }

    private fun getBeanAnnotations(): Set<PsiClass> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getBeanAnnotationsInner(),
                ModificationTrackerManager.getInstance(project).getLibraryTracker()
            )
        }
    }

    private fun getBeanAnnotationsInner(): Set<PsiClass> {
        val searchScope = GlobalSearchScope.allScope(project)

        return QuarkusCoreClasses.COMPONENTS_ANNO.asSequence()
            .mapNotNull { LibraryClassCache.searchForLibraryClass(project, it) }
            .flatMap { MetaAnnotationUtil.getChildren(it, searchScope) + it }
            .toSet()
    }

    private fun findProducesBeans(psiBean: PsiBean): List<PsiBean> {
        val psiClass = psiBean.psiClass.takeIf { it.isValid } ?: return emptyList()
        val methodBeans = psiClass.allMethods.asSequence()
            .filter { it.isNonPrivate }
            .filter { it.isMetaAnnotatedBy(QuarkusCoreClasses.PRODUCES.allFqns) }
            .mapNotNull { method ->
                //todo array
                method.returnType?.resolvedPsiClass?.let { psiClass ->
                    PsiBean(method.name, psiClass, null, method) //todo qualifiers
                }
            }
        val fieldBeans = psiClass.allFields.asSequence()
            .filter { it.isMetaAnnotatedBy(QuarkusCoreClasses.PRODUCES.allFqns) }
            .mapNotNull { field ->
                field.type.resolvedPsiClass?.let { psiClass ->
                    PsiBean(field.name, psiClass, null, field) //todo qualifiers
                }
            }
        return (methodBeans + fieldBeans).toList()
    }

    private fun getTypeForSearch(uVariable: UVariable): PsiType? {
        val psiVariable = uVariable.javaPsi as? PsiModifierListOwner ?: return null
        if (uVariable.type is PsiArrayType) return uVariable.type
        val type = uVariable.type as? PsiClassType ?: return null

        if (psiVariable.isMetaAnnotatedBy(QuarkusCoreClasses.ALL)) {
            if (!type.isInheritorOf(List::class.java.canonicalName)) return null
            return type.parameters.firstOrNull()
        }

        if (QuarkusCoreClasses.INSTANCE.allFqns.any { type.isInheritorOf(it) }) {
            return type.parameters.firstOrNull()
        }

        if (type.isInheritorOf(java.util.Optional::class.java.canonicalName)) {
            return type.parameters.firstOrNull() ?: return type
        }
        return type
    }

    private fun isMultiBeans(uVariable: UVariable): Boolean {
        val psiVariable = uVariable.javaPsi as? PsiModifierListOwner ?: return false
        val type = uVariable.type as? PsiClassType ?: return false

        if (psiVariable.isMetaAnnotatedBy(QuarkusCoreClasses.ALL)) {
            return type.isInheritorOf(List::class.java.canonicalName)
        }

        return QuarkusCoreClasses.INSTANCE.allFqns.any { type.isInheritorOf(it) }
    }

    fun findActiveBeanDeclarations(
        allPsiBeans: Collection<PsiBean>,
        uVariable: UVariable,
        qualifier: PsiAnnotation?
    ): List<PsiMember> {
        val psiTypeForSearch = getTypeForSearch(uVariable)
        val sourcePsiType = uVariable.type
        val beanNameFromQualifier = qualifier?.resolveBeanName()
        val isMultipleBean = AtomicBoolean(isMultiBeans(uVariable))

        val resultByType: List<PsiBean> = if (psiTypeForSearch != null) {
            findActiveBeanDeclarations(
                allPsiBeans, isMultipleBean, sourcePsiType, psiTypeForSearch, qualifier, beanNameFromQualifier
            )
        } else {
            allPsiBeans.filter { it.filterByQualifier(qualifier, beanNameFromQualifier) }
        }

        if (resultByType.size == 1) {
            return resultByType.map { it.psiMember }
        }

        if (qualifier == null) {
            return resultByType.map { it.psiMember }
        }
        val byNameFromQualifierBean = resultByType
            .filter { it.filterByQualifier(qualifier, beanNameFromQualifier) }
            .map { it.psiMember }
        return byNameFromQualifierBean.ifEmpty { byNameFromQualifierBean }
    }

    private fun findActiveBeanDeclarations(
        allPsiBeans: Collection<PsiBean>,
        atomicIsMultipleBean: AtomicBoolean,
        sourcePsiType: PsiType,
        psiTypeForSearch: PsiType,
        qualifier: PsiAnnotation?,
        beanNameFromQualifier: String?
    ): List<PsiBean> {
        //it.isAssigFrom todo try it for isExactMatch

        val memberPsiBeans = allPsiBeans.filter { it.isMember() }
        val byExactMatchBeans = memberPsiBeans.filter { getProducesMemberType(it)?.isExactMatch(sourcePsiType) == true }
        var byTypeMemberBeans = byExactMatchBeans
        if (atomicIsMultipleBean.get() || byExactMatchBeans.isEmpty()) {
            byTypeMemberBeans = memberPsiBeans.filter {
                getProducesMemberType(it)?.isEqualOrInheritorType(psiTypeForSearch) == true
            }
        }

        val byTypeClassBeans = getPsiClassesByComponents(
            allPsiBeans, sourcePsiType, psiTypeForSearch, byTypeMemberBeans.isEmpty()
        ).toSet()

        if (atomicIsMultipleBean.get() && byExactMatchBeans.isNotEmpty()
            && byTypeMemberBeans.isEmpty() && byTypeClassBeans.isEmpty()
        ) {
            // Check if multiple bean has exact type match
            atomicIsMultipleBean.set(false)
            byTypeMemberBeans = byExactMatchBeans
        }

        val foundPsiBeans = byTypeMemberBeans + byTypeClassBeans

        val aResultByTypeAndQualifier: List<PsiBean> = foundPsiBeans
            .filter { it.filterByQualifier(qualifier, beanNameFromQualifier) }

        if (aResultByTypeAndQualifier.isEmpty()) {
            return emptyList()
        }

        if (aResultByTypeAndQualifier.size > 1 && !atomicIsMultipleBean.get()) {
            val byPrimary =
                aResultByTypeAndQualifier.filter { !it.psiMember.isMetaAnnotatedBy(QuarkusCoreClasses.DEFAULT_BEAN) }
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

    private fun getProducesMemberType(bean: PsiBean): PsiType? {
        return when (bean.psiMember) {
            is PsiMethod -> (bean.psiMember as PsiMethod).returnType
            is PsiField -> (bean.psiMember as PsiField).type
            else -> null
        }
    }

    private fun getPsiClassesByComponents(
        allPsiBeans: Collection<PsiBean>,
        sourcePsiType: PsiType,
        beanPsiType: PsiType,
        byTypeBeanMethodsIsEmpty: Boolean
    ): List<PsiBean> {
        val beanPsiClass = beanPsiType.resolvedPsiClass
        val componentPsiBeans = allPsiBeans.filter { !it.isMember() }
        return componentPsiBeans.asSequence()
            .filter {
                beanPsiClass != null && it.psiClass.isEqualOrInheritor(beanPsiClass)
                        || beanPsiType is PsiWildcardType && it.psiClass.matchesWildcardType(beanPsiType)
            }
            .filter { !it.psiClass.isGeneric(sourcePsiType) || !byTypeBeanMethodsIsEmpty }
            .toList()
    }

    companion object {
        fun getInstance(project: Project): QuarkusSearchService = project.service()
    }
}