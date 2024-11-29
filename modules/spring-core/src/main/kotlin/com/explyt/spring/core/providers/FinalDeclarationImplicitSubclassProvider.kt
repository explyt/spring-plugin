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

package com.explyt.spring.core.providers

import com.explyt.spring.core.JavaEeClasses
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringCoreClasses.ASYNC
import com.explyt.spring.core.SpringCoreClasses.BEAN
import com.explyt.spring.core.SpringCoreClasses.CACHEABLE
import com.explyt.spring.core.SpringCoreClasses.CACHECONFIG
import com.explyt.spring.core.SpringCoreClasses.CACHEEVICT
import com.explyt.spring.core.SpringCoreClasses.CACHEPUT
import com.explyt.spring.core.SpringCoreClasses.CACHING
import com.explyt.spring.core.SpringCoreClasses.CONFIGURATION
import com.explyt.spring.core.util.SpringCoreUtil.isSpringBeanCandidateClass
import com.explyt.util.ExplytPsiUtil.getMetaAnnotation
import com.explyt.util.ExplytPsiUtil.isAbstract
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isStatic
import com.intellij.codeInspection.inheritance.ImplicitSubclassProvider
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.roots.TestSourcesFilter
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

@Suppress("UnstableApiUsage")
class FinalDeclarationImplicitSubclassProvider : ImplicitSubclassProvider() {

    private val transactionalAnnotations = listOf(SpringCoreClasses.TRANSACTIONAL) + JavaEeClasses.TRANSACTIONAL.allFqns
    private val cacheableAnnotations = listOf(CACHEABLE, CACHING, CACHEEVICT, CACHEPUT, CACHECONFIG)
    private val overrideMethods = listOf(ASYNC) + cacheableAnnotations
    private val overrideClasses = listOf(ASYNC, CONFIGURATION) + transactionalAnnotations + cacheableAnnotations

    override fun getSubclassingInfo(psiClass: PsiClass): SubclassingInfo? {
        val methods = psiClass.methods

        val methodsInfo = mutableMapOf<PsiMethod, OverridingInfo>()
        for (method in methods) {
            val overridingInfo = getOverridingInfoForMethod(method)
            if (overridingInfo != null) {
                methodsInfo[method] = overridingInfo
            }
        }

        val existAnnotation = psiClass.modifierList?.annotations?.isNotEmpty() ?: false
        if (existAnnotation) {
            val annotation = overrideClasses.asSequence()
                .map { psiClass.getMetaAnnotation(it) }
                .filterNotNull()
                .firstOrNull()
            if ( annotation != null ) {
                val shortName = annotation.qualifiedName?.split(".")?.last() ?: ""
                return SubclassingInfo(
                    SpringCoreBundle.message("explyt.implicit.inspection.forClass.annotated", shortName),
                    methodsInfo.ifEmpty { null },
                    psiClass.isAbstract)
            }

        }

        if (methodsInfo.isNotEmpty()) {
            val className = psiClass.name ?: return null
            return SubclassingInfo(
                SpringCoreBundle.message("explyt.implicit.inspection.subclass.display.forClass", className),
                methodsInfo,
                psiClass.isAbstract
            )
        }
        return null
    }

    override fun isApplicableTo(psiClass: PsiClass): Boolean {
        return isSpringBeanCandidateClass(psiClass)
    }

    private fun getOverridingInfoForMethod(method: PsiMethod): OverridingInfo? {
        val annotations = method.modifierList.annotations
        if (annotations.isEmpty()) {
            return null
        }

        val annotation = overrideMethods.asSequence()
            .map { method.getMetaAnnotation(it) }
            .filterNotNull()
            .firstOrNull()
        return if (annotation != null) {
            val shortName = annotation.qualifiedName?.split(".")?.last() ?: ""
            val message =
                SpringCoreBundle.message("explyt.implicit.inspection.forMethod.annotated", shortName)
            OverridingInfo(message)
        } else if (isBeanInConfiguration(method)) {
            OverridingInfo(SpringCoreBundle.message("explyt.implicit.inspection.bean.in.configuration"))
        } else {
            getOverridingInfoForTransactional(method)
        }
    }

    private fun isBeanInConfiguration(method: PsiMethod): Boolean {
        if (method.isStatic) {
            return false
        }
        val psiClass = method.containingClass ?: return false
        return psiClass.isMetaAnnotatedBy(CONFIGURATION) && method.isMetaAnnotatedBy(BEAN)
    }

    private fun isTestFiles(psiElement: PsiElement): Boolean {
        val virtualFile = psiElement.containingFile?.virtualFile ?: return false
        return TestSourcesFilter.isTestSources(virtualFile, psiElement.project)
    }

    private fun getModifiersForTransactional(method: PsiMethod): Array<JvmModifier> {
        return if (isTestFiles(method)) {
            arrayOf(JvmModifier.PUBLIC, JvmModifier.PROTECTED, JvmModifier.PACKAGE_LOCAL)
        } else {
            arrayOf(JvmModifier.PUBLIC)
        }
    }

    private fun getOverridingInfoForTransactional(method: PsiMethod): OverridingInfo? {
        val acceptedModifiers: Array<JvmModifier> = getModifiersForTransactional(method)
        val annotation = transactionalAnnotations.asSequence()
            .map { method.getMetaAnnotation(it) }
            .filterNotNull()
            .firstOrNull() ?: return null

        val shortName = annotation.qualifiedName?.split(".")?.last() ?: ""
        return OverridingInfo(
            SpringCoreBundle.message("explyt.implicit.inspection.forMethod.annotated", shortName),
            acceptedModifiers
        )
    }

}