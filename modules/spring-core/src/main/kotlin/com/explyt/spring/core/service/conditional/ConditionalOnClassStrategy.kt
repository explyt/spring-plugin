/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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