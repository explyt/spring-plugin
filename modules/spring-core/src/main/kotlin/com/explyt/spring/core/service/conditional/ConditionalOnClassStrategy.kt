/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.MetaAnnotationsHolder
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

class ConditionalOnClassStrategy(private val module: Module) : AnnotationConditionStrategy(
    listOf(SpringSearchService.getInstance(module.project).getMetaAnnotations(module, SpringCoreClasses.CONDITIONAL_ON_CLASS)),
    setOf(ConditionAssumption.COMPILE_CLASSPATH_IS_RUNTIME)
) {

    override fun unmetRequirement(
        holder: MetaAnnotationsHolder, carrier: PsiMember, activeBeans: Collection<PsiBean>
    ): String? {
        val unresolvedClass = holder.getAnnotationMemberValues(carrier, setOf("value"))
            .asSequence()
            .flatMap { it.childrenOfType<PsiTypeElement>() }
            .firstOrNull { it.type.resolvedPsiClass == null }
        if (unresolvedClass != null) return "class ${unresolvedClass.text} is not on the classpath"

        return holder.getAnnotationMemberValues(carrier, setOf("name"))
            .asSequence()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .distinct()
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
