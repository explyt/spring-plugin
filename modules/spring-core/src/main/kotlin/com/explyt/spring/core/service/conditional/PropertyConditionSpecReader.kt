/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiMember
import com.intellij.psi.util.PsiTreeUtil

class PropertyConditionSpecReader(private val propertyAnnotations: MetaAnnotationsHolder) {

    fun read(member: PsiMember): List<PropertyConditionSpec> =
        (member.annotations.asSequence() + PsiTreeUtil.findChildrenOfType(member, PsiAnnotation::class.java).asSequence())
            .distinctBy { it.textRange }
            .flatMap { specsOf(it) }
            .toList()

    private fun specsOf(annotation: PsiAnnotation): List<PropertyConditionSpec> = when (annotation.qualifiedName) {
        SpringCoreClasses.CONDITIONAL_ON_PROPERTIES,
        SpringCoreClasses.CONDITIONAL_ON_BOOLEAN_PROPERTIES -> repeated(annotation).flatMap { specsOf(it) }

        SpringCoreClasses.CONDITIONAL_ON_BOOLEAN_PROPERTY -> listOf(booleanSpec(annotation))
        else -> if (propertyAnnotations.contains(annotation)) listOf(propertySpec(annotation)) else emptyList()
    }

    @Suppress("DEPRECATION")
    private fun propertySpec(annotation: PsiAnnotation) = PropertyConditionSpec.of(
        prefix = strings(propertyAnnotations.getAnnotationMemberValues(annotation, setOf(PREFIX))).firstOrNull(),
        names = strings(propertyAnnotations.getAnnotationMemberValues(annotation, setOf(NAME, VALUE))),
        havingValue = strings(propertyAnnotations.getAnnotationMemberValues(annotation, setOf(HAVING_VALUE))).firstOrNull(),
        matchIfMissing = AnnotationUtil.getBooleanAttributeValue(annotation, MATCH_IF_MISSING) ?: false
    )

    private fun booleanSpec(annotation: PsiAnnotation) = PropertyConditionSpec.of(
        prefix = AnnotationUtil.getStringAttributeValue(annotation, PREFIX),
        names = strings(listOfNotNull(
            annotation.findDeclaredAttributeValue(NAME),
            annotation.findDeclaredAttributeValue(VALUE)
        )),
        havingValue = (AnnotationUtil.getBooleanAttributeValue(annotation, HAVING_VALUE) ?: true).toString(),
        matchIfMissing = AnnotationUtil.getBooleanAttributeValue(annotation, MATCH_IF_MISSING) ?: false
    )

    private fun repeated(container: PsiAnnotation): List<PsiAnnotation> =
        container.findDeclaredAttributeValue(VALUE)
            ?.let { value ->
                flatten(value).filterIsInstance<PsiAnnotation>()
                    .ifEmpty { PsiTreeUtil.findChildrenOfType(value, PsiAnnotation::class.java).toList() }
            }
            .orEmpty()

    private fun strings(values: Collection<PsiAnnotationMemberValue>): List<String> =
        values.flatMap { flatten(it) }.mapNotNull { AnnotationUtil.getStringAttributeValue(it) }.distinct()

    private fun flatten(value: PsiAnnotationMemberValue): List<PsiAnnotationMemberValue> =
        if (value is PsiArrayInitializerMemberValue) value.initializers.flatMap { flatten(it) } else listOf(value)

    private companion object {
        const val PREFIX = "prefix"
        const val NAME = "name"
        const val VALUE = "value"
        const val HAVING_VALUE = "havingValue"
        const val MATCH_IF_MISSING = "matchIfMissing"
    }
}
