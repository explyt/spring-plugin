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
import com.explyt.spring.core.util.SpringCoreUtil.beanPsiType
import com.explyt.spring.core.util.SpringCoreUtil.getBeanName
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanName
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.explyt.spring.core.util.SpringCoreUtil.resolvePsiClass
import com.explyt.util.ExplytPsiUtil.isGeneric
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*

@Service(Service.Level.PROJECT)
class SpringBeanService {

    fun getBeanCandidates(
        module: Module,
        psiType: PsiType,
        beanName: String
    ): Set<PsiBean> {
        val beanPsiType = psiType.beanPsiType ?: return emptySet()
        val beanPsiClass = psiType.resolveBeanPsiClass ?: return emptySet()
        val searchServiceFacade = SpringSearchServiceFacade.getInstance(module.project)

        val excludedBeans = searchServiceFacade.getExcludedBeansClasses(module)
        val classInheritors = SpringSearchUtils.searchClassInheritors(beanPsiClass)
            .asSequence()
            .filter { it.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT) }
            .filter { inheritor -> excludedBeans.none { it.psiMember == inheritor } }
            .toSet()
        val allBeansPsiMethods = searchServiceFacade.getComponentBeanPsiMethods(module)
            .filterTo(mutableSetOf()) { psiMethod -> excludedBeans.none { it.psiMember == psiMethod } }
        val beansPsiMethods = SpringSearchUtils
            .getBeansPsiMethodsCheckMultipleBean(psiType, allBeansPsiMethods, beanPsiType).toSet()

        val beanCandidatesByComponent = getBeanCandidatesInPsiModifierListOwner(module, classInheritors, SpringCoreClasses.COMPONENT)
        val beanCandidatesByMethod = getBeanCandidatesInPsiModifierListOwner(module, beansPsiMethods, SpringCoreClasses.BEAN)

        var beanCandidatesByResolveClass: Set<PsiBean> = setOf()
        if (beanPsiClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)
            && (!beanPsiClass.isGeneric(psiType) || beansPsiMethods.isEmpty())) {
            beanCandidatesByResolveClass = getBeanCandidatesInPsiModifierListOwner(module, setOf(beanPsiClass), SpringCoreClasses.COMPONENT)
        }
        return filterBeanCandidates(module, beanName,
            beanCandidatesByComponent + beanCandidatesByResolveClass, beanCandidatesByMethod)
    }

    private fun getBeanCandidatesInPsiModifierListOwner(
        module: Module,
        owners: Set<PsiMember>,
        annotationName: String
    ): Set<PsiBean> {
        val beanCandidates = mutableSetOf<PsiBean>()
        owners.forEach { owner ->
            val psiClass = if (owner is PsiMethod) {
                owner.returnType?.resolveBeanPsiClass
            } else {
                owner.resolvePsiClass
            }
            if (psiClass != null) {
                val isPrimary = owner.isMetaAnnotatedBy(SpringCoreClasses.PRIMARY)
                val beanQualifier = SpringCoreClasses.QUALIFIERS
                    .asSequence()
                    .mapNotNull { owner.getAnnotationValue(module, it) }
                    .map { value -> PsiBean(value, psiClass, null, owner, isPrimary) }
                    .toMutableSet()
                beanCandidates += beanQualifier

                val beanNameAnnotationValue = owner.getAnnotationValue(module, annotationName)
                if (beanQualifier.isEmpty() && !beanNameAnnotationValue.isNullOrBlank()) {
                    beanCandidates += PsiBean(beanNameAnnotationValue, psiClass, null, owner, isPrimary)
                }

                if (beanQualifier.isEmpty() && beanNameAnnotationValue.isNullOrBlank()) {
                    val beanNames = when (owner) {
                        is PsiClass -> owner.getBeanName()?.let { setOf(it) }
                        is PsiMethod -> owner.resolveBeanName
                        else -> null
                    }
                    if (beanNames != null) {
                        beanCandidates += beanNames.map { PsiBean(it, psiClass, null, owner, isPrimary) }
                    }
                }
            }
        }
        return beanCandidates
    }

    private fun filterBeanCandidates(
        module: Module,
        beanName: String,
        beanCandidatesByComponent: Set<PsiBean>,
        beanCandidatesByMethod: Set<PsiBean>
    ): Set<PsiBean> {
        val metaHolder = SpringSearchService.getInstance(module.project).getMetaAnnotations(module, SpringCoreClasses.COMPONENT)

        val isComponentPrimary = beanCandidatesByComponent.any { it.isPrimary }
        val isMethodPrimary = beanCandidatesByMethod.any { it.isPrimary }
        val isExistComponentName = beanCandidatesByComponent.any {
            metaHolder.getAnnotationMemberValues(it.psiMember, setOf("value")).isNotEmpty()
        }

        if (isComponentPrimary) {
            return if (isExistComponentName) {
                if (isMethodPrimary) {
                    // return component primary with name and bean primary
                    (beanCandidatesByComponent.asSequence().filter { it.isPrimary } + beanCandidatesByMethod.asSequence().filter { it.isPrimary }).toSet()
                } else {
                    // return only component primary with name (bean not primary)
                    beanCandidatesByComponent.asSequence().filter { it.isPrimary }.toSet()
                }
            } else {
                if (isMethodPrimary) {
                    // return only method primary (component primary (without name))
                    beanCandidatesByMethod.asSequence().filter { it.isPrimary }.toSet()
                } else {
                    // return component primary and method
                    beanCandidatesByComponent.asSequence().filter { it.isPrimary }.toSet() + beanCandidatesByMethod
                }
            }
        } else if (isMethodPrimary) {
            // return method primary
            return beanCandidatesByMethod.asSequence().filter { it.isPrimary }.toSet()
        }
        val byNameBeanCandidateMethods = beanCandidatesByMethod.asSequence().filter { it.name == beanName }.toSet()
        val byNameBeanCandidateComponents = beanCandidatesByComponent.asSequence().filter { it.name == beanName }.toSet()
        if (byNameBeanCandidateMethods.size == 1 && byNameBeanCandidateComponents.size == 1) {
            // return only method by name
            return byNameBeanCandidateMethods
        }
        // return all components and methods
        return beanCandidatesByComponent + beanCandidatesByMethod
    }

    private fun PsiModifierListOwner.getAnnotationValue(module: Module, annotationName: String): String? {
        if (this is PsiMember) {
            val metaHolder = SpringSearchService.getInstance(module.project).getMetaAnnotations(module, annotationName)
            val annotationValue = metaHolder.getAnnotationMemberValues(this, setOf("value")).firstOrNull()
            return annotationValue?.let { AnnotationUtil.getStringAttributeValue(it) }
        }
        return null
    }

    companion object {
        fun getInstance(project: Project): SpringBeanService = project.service()
    }
}