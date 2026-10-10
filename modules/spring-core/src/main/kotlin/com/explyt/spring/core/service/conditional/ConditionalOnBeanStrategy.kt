/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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

class ConditionalOnBeanStrategy(module: Module) : AnnotationConditionStrategy(
    listOf(SpringCoreClasses.CONDITIONAL_ON_BEAN, SpringCoreClasses.CONDITIONAL_ON_SINGLE_BEAN)
        .map { SpringSearchService.getInstance(module.project).getMetaAnnotations(module, it) },
    setOf(ConditionAssumption.STATIC_BEAN_MODEL_COMPLETE)
) {

    override fun unmetRequirement(
        holder: MetaAnnotationsHolder, carrier: PsiMember, activeBeans: Collection<PsiBean>
    ): String? {
        val foundBeanNames = activeBeans.mapTo(mutableSetOf()) { it.name }

        val names = holder.getAnnotationMemberValues(carrier, setOf("name"))
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .toSet()
        names.firstOrNull { it !in foundBeanNames }?.let { return "no bean named $it" }

        val foundBeanClassQn = activeBeans.mapNotNullTo(mutableSetOf()) { it.psiClass.qualifiedName }
        val types = holder.getAnnotationMemberValues(carrier, setOf("type"))
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .toSet()
        types.firstOrNull { it !in foundBeanClassQn }?.let { return "no bean of type $it" }

        val classAttributes = holder.getAnnotationMemberValues(carrier, setOf("value"))
        val classesQn = if (names.isEmpty() && types.isEmpty() && classAttributes.isEmpty()) {
            setOfNotNull(carrier.resolvePsiClass?.qualifiedName)
        } else {
            val typeNames = PsiAnnotationUtils.getTypeNames(classAttributes)
            if (classAttributes.size != typeNames.size) return UNRESOLVED_TYPE
            typeNames
        }
        if (classAttributes.isNotEmpty() && classesQn.isEmpty()) return UNRESOLVED_TYPE
        return classesQn.firstOrNull { it !in foundBeanClassQn }?.let { "no bean of type $it" }
    }

    private companion object {
        const val UNRESOLVED_TYPE = "bean type cannot be resolved"
    }
}
