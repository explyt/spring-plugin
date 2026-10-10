/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchService
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMember
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache

class ConditionalOnClassStrategy(private val module: Module) : AnnotationConditionStrategy(
    listOf(SpringSearchService.getInstance(module.project).getMetaAnnotations(module, SpringCoreClasses.CONDITIONAL_ON_CLASS)),
    setOf(ConditionAssumption.COMPILE_CLASSPATH_IS_RUNTIME)
) {

    override fun unmetRequirement(
        holder: MetaAnnotationsHolder, carrier: PsiMember, activeBeans: Collection<PsiBean>
    ): String? {
        val references = ConditionClassReferences(holder, carrier)
        val unresolvedClass = references.unresolvedClassLiteral("value")
        if (unresolvedClass != null) return "class $unresolvedClass is not on the classpath"

        return references.classNames("name")
            .firstOrNull { !module.hasClass(it) }
            ?.let { "class $it is not on the classpath" }
    }
}

internal fun Module.hasClass(qualifiedName: String): Boolean {
    val className = qualifiedName.split('.').last()
    return PsiShortNamesCache.getInstance(project)
        .getClassesByName(className, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(this))
        .any { it.qualifiedName == qualifiedName }
}
