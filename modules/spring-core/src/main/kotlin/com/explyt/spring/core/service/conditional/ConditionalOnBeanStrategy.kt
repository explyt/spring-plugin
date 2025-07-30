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

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.util.PsiAnnotationUtils
import com.explyt.spring.core.util.SpringCoreUtil.resolvePsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMember

class ConditionalOnBeanStrategy(module: Module) : ExclusionStrategy {
    private val annotationBeanHolder = SpringSearchService.getInstance(module.project)
        .getMetaAnnotations(module, SpringCoreClasses.CONDITIONAL_ON_BEAN)
    private val annotationSingleHolder = SpringSearchService.getInstance(module.project)
        .getMetaAnnotations(module, SpringCoreClasses.CONDITIONAL_ON_SINGLE_BEAN)

    override fun shouldExclude(dependant: PsiMember, foundBeans: Collection<PsiBean>): Boolean {
        val exclude = shouldExclude(annotationBeanHolder, dependant, foundBeans)
        if (exclude) return true

        val excludeSingle = shouldExclude(annotationSingleHolder, dependant, foundBeans)
        return excludeSingle
    }

    private fun shouldExclude(
        annotationHolder: MetaAnnotationsHolder, dependant: PsiMember, foundBeans: Collection<PsiBean>,
    ): Boolean {
        if (dependant.annotations.none { annotationHolder.contains(it) }) {
            return false
        }
        val foundBeanNames = foundBeans.mapTo(mutableSetOf()) { it.name }

        val names = annotationHolder.getAnnotationMemberValues(dependant, setOf("name"))
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .toSet()
        if (names.isNotEmpty() && names.any { !foundBeanNames.contains(it) }) {
            return true
        }

        val foundBeanClassQn = foundBeans.mapNotNullTo(mutableSetOf()) { it.psiClass.qualifiedName }
        val types = annotationHolder.getAnnotationMemberValues(dependant, setOf("type"))
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .toSet()
        if (types.isNotEmpty() && types.any { !foundBeanClassQn.contains(it) }) {
            return true
        }

        val classAttributes = annotationHolder.getAnnotationMemberValues(dependant, setOf("value"))
        val classesQn = if (names.isEmpty() && types.isEmpty() && classAttributes.isEmpty()) {
            setOfNotNull(dependant.resolvePsiClass?.qualifiedName)
        } else {
            val typeNames = PsiAnnotationUtils.getTypeNames(classAttributes)
            if (classAttributes.size != typeNames.size) {
                return true
            }
            typeNames
        }
        if (classAttributes.isNotEmpty() && classesQn.isEmpty()) {
            return true
        }
        return classesQn.isNotEmpty() && classesQn.any { !foundBeanClassQn.contains(it) }
    }

}