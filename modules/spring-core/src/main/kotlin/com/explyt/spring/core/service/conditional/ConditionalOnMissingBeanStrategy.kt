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
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.util.PsiAnnotationUtils
import com.explyt.spring.core.util.SpringCoreUtil.resolvePsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMember

class ConditionalOnMissingBeanStrategy(module: Module) : ExclusionStrategy {
    private val annotationHolder = SpringSearchService.getInstance(module.project)
        .getMetaAnnotations(module, SpringCoreClasses.CONDITIONAL_ON_MISSING_BEAN)

    override fun shouldExclude(dependant: PsiMember, foundBeans: Collection<PsiBean>): Boolean {
        if (dependant.annotations.none { annotationHolder.contains(it) }) {
            return false
        }

        val names = annotationHolder.getAnnotationMemberValues(dependant, setOf("name"))
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .toSet()
        if (names.isNotEmpty() && foundBeans.any { names.contains(it.name) }) {
            return true
        }

        val types = annotationHolder.getAnnotationMemberValues(dependant, setOf("type"))
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .toSet()
        if (types.isNotEmpty() && foundBeans.any { types.contains(it.psiClass.qualifiedName) }) {
            return true
        }

        val classAttributes = annotationHolder.getAnnotationMemberValues(dependant, setOf("value"))
        val classesQn = if (names.isEmpty() && types.isEmpty() && classAttributes.isEmpty()) {
            setOfNotNull(dependant.resolvePsiClass?.qualifiedName)
        } else {
            PsiAnnotationUtils.getTypeNames(classAttributes)
        }
        return classesQn.isNotEmpty() && foundBeans.all { classesQn.contains(it.psiClass.qualifiedName) }
    }

}