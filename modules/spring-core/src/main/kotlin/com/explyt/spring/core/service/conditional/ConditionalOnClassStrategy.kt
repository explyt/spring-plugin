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
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.childrenOfType

class ConditionalOnClassStrategy(private val module: Module) : ExclusionStrategy {
    private val annotationHolder = SpringSearchService.getInstance(module.project)
        .getMetaAnnotations(module, SpringCoreClasses.CONDITIONAL_ON_CLASS)

    override fun shouldExclude(dependant: PsiMember, foundBeans: Collection<PsiBean>): Boolean {
        if (dependant.annotations.none { annotationHolder.contains(it) }) {
            return false
        }

        val classAttributeTypes = annotationHolder.getAnnotationMemberValues(dependant, setOf("value"))
            .asSequence()
            .flatMap { it.childrenOfType<PsiTypeElement>() }
            .map { it.type }
            .toList()

        if (classAttributeTypes.isNotEmpty() && classAttributeTypes.any { it.resolvedPsiClass == null }) {
            return true
        }

        val typesQn = annotationHolder.getAnnotationMemberValues(dependant, setOf("name"))
            .asSequence()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .toSet()
            .takeIf { it.isNotEmpty() } ?: return false

        for (typeQn in typesQn) {
            val className = typeQn.split('.').lastOrNull() ?: continue

            val classFound = PsiShortNamesCache.getInstance(module.project)
                .getClassesByName(
                    className,
                    GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
                )
                .any { it.qualifiedName == typeQn }

            if (!classFound) return true
        }

        return false
    }

}