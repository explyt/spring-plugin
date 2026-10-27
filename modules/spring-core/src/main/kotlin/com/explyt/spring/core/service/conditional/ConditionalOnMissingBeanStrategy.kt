/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.util.SpringCoreUtil.resolvePsiClass
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiMember

class ConditionalOnMissingBeanStrategy(module: Module) : AnnotationConditionStrategy(
    listOf(
        SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringCoreClasses.CONDITIONAL_ON_MISSING_BEAN)
    ),
    setOf(ConditionAssumption.STATIC_BEAN_MODEL_COMPLETE)
) {

    override fun unmetRequirement(
        holder: MetaAnnotationsHolder, carrier: PsiMember, activeBeans: Collection<PsiBean>
    ): String? {
        val names = holder.mergedValuesOf(carrier, setOf("name")).mapNotNullTo(mutableSetOf()) { it.stringValue }
        activeBeans.firstOrNull { it.name in names }?.let { return "bean named ${it.name} exists" }

        val types = holder.mergedValuesOf(carrier, setOf("type")).mapNotNullTo(mutableSetOf()) { it.stringValue }
        activeBeans.firstOrNull { it.psiClass.qualifiedName in types }
            ?.let { return "bean of type ${it.psiClass.qualifiedName} exists" }

        val classAttributes = holder.mergedValuesOf(carrier, setOf("value"))
        val classesQn = if (names.isEmpty() && types.isEmpty() && classAttributes.isEmpty()) {
            setOfNotNull(carrier.resolvePsiClass?.qualifiedName)
        } else {
            classAttributes.mapNotNullTo(mutableSetOf()) { it.classQualifiedName }
        }
        if (classesQn.isEmpty()) return null
        return activeBeans.asSequence()
            .filter { isNotSame(it, carrier) }
            .firstOrNull { it.psiClass.qualifiedName in classesQn }
            ?.let { "bean ${it.name} of type ${it.psiClass.qualifiedName} exists" }
    }

    private fun isNotSame(bean: PsiBean, carrier: PsiMember): Boolean {
        if (bean.psiMember == carrier) return false
        return bean.psiMember.containingClass != carrier
    }
}
