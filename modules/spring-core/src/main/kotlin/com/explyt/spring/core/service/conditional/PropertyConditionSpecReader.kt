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
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.toUElementOfType

class PropertyConditionSpecReader(private val propertyAnnotations: MetaAnnotationsHolder) {

    fun read(member: PsiMember): List<PropertyConditionSpec> = member.annotations.flatMap { specsOf(it) }

    private fun specsOf(annotation: PsiAnnotation, visited: Set<String> = emptySet()): List<PropertyConditionSpec> {
        val name = annotation.qualifiedName ?: return emptyList()
        if (name in visited) return emptyList()
        val next = visited + name
        return when (name) {
            SpringCoreClasses.CONDITIONAL_ON_PROPERTIES,
            SpringCoreClasses.CONDITIONAL_ON_BOOLEAN_PROPERTIES -> repeated(annotation).flatMap { specsOf(it, next) }

            SpringCoreClasses.CONDITIONAL_ON_BOOLEAN_PROPERTY -> listOf(booleanSpec(annotation))
            else -> if (propertyAnnotations.contains(annotation)) listOf(propertySpec(annotation))
            else annotation.resolveAnnotationType()?.annotations.orEmpty().flatMap { specsOf(it, next) }
        }
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

    private fun repeated(container: PsiAnnotation): List<PsiAnnotation> {
        val nested = container.findAttributeValue(VALUE)?.let { flatten(it) }
            .orEmpty().filterIsInstance<PsiAnnotation>()
        if (nested.isNotEmpty()) return nested
        val value = container.toUElementOfType<UAnnotation>()?.findAttributeValue(VALUE)
        return MetaAnnotationsHolder.getValues(value).mapNotNull {
            (it as? UAnnotation)?.javaPsi ?: it.sourcePsi.toUElementOfType<UAnnotation>()?.javaPsi
        }
    }

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
