/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMember
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.toUElementOfType

class PropertyConditionSpecReader(private val propertyAnnotations: MetaAnnotationsHolder) {
    private val metaSpecsByAnnotation = HashMap<String, List<PropertyConditionSpec>>()

    fun read(member: PsiMember): List<Pair<PsiAnnotation, List<PropertyConditionSpec>>> =
        member.annotations.mapNotNull { annotation -> read(annotation).takeIf { it.isNotEmpty() }?.let { annotation to it } }

    fun read(annotation: PsiAnnotation): List<PropertyConditionSpec> = specsOf(annotation)

    private fun specsOf(annotation: PsiAnnotation, visited: Set<String> = emptySet()): List<PropertyConditionSpec> {
        val name = annotation.qualifiedName ?: return emptyList()
        if (name in visited) return emptyList()
        return when (name) {
            SpringCoreClasses.CONDITIONAL_ON_PROPERTIES,
            SpringCoreClasses.CONDITIONAL_ON_BOOLEAN_PROPERTIES -> repeated(annotation).flatMap { specsOf(it, visited + name) }

            SpringCoreClasses.CONDITIONAL_ON_BOOLEAN_PROPERTY -> listOf(booleanSpec(annotation))
            else -> if (propertyAnnotations.contains(annotation)) listOf(propertySpec(annotation))
            else metaSpecsOf(annotation, name, visited)
        }
    }

    private fun metaSpecsOf(annotation: PsiAnnotation, name: String, visited: Set<String>): List<PropertyConditionSpec> {
        if (PLATFORM_ANNOTATION_PACKAGES.any { name.startsWith(it) }) return emptyList()
        if (visited.isNotEmpty()) return metaAnnotationSpecs(annotation, visited + name)
        return metaSpecsByAnnotation.getOrPut(name) { metaAnnotationSpecs(annotation, setOf(name)) }
    }

    private fun metaAnnotationSpecs(annotation: PsiAnnotation, visited: Set<String>): List<PropertyConditionSpec> =
        annotation.resolveAnnotationType()?.annotations.orEmpty().flatMap { specsOf(it, visited) }

    @Suppress("DEPRECATION")
    private fun propertySpec(annotation: PsiAnnotation) = PropertyConditionSpec.of(
        prefix = strings(propertyAnnotations.getAnnotationMemberValues(annotation, setOf(PREFIX))).firstOrNull(),
        names = strings(propertyAnnotations.getAnnotationMemberValues(annotation, setOf(NAME, VALUE))),
        havingValue = strings(propertyAnnotations.getAnnotationMemberValues(annotation, setOf(HAVING_VALUE))).firstOrNull(),
        matchIfMissing = propertyAnnotations.getAnnotationMemberValues(annotation, setOf(MATCH_IF_MISSING))
            .firstNotNullOfOrNull { booleanConstant(it) } ?: false,
        unreadableNames = hasUnreadableSourceValue(annotation, setOf(PREFIX, NAME, VALUE))
    )

    private fun hasUnreadableSourceValue(annotation: PsiAnnotation, attributes: Set<String>): Boolean {
        val source = annotation.toUElementOfType<UAnnotation>() ?: return false
        return propertyAnnotations.getAnnotationMemberValues(source, attributes)
            .flatMap { MetaAnnotationsHolder.getValues(it) }
            .any { it.evaluate() !is String }
    }

    private fun booleanSpec(annotation: PsiAnnotation) = PropertyConditionSpec.of(
        prefix = AnnotationUtil.getStringAttributeValue(annotation, PREFIX),
        names = strings(listOfNotNull(
            annotation.findDeclaredAttributeValue(NAME),
            annotation.findDeclaredAttributeValue(VALUE)
        )),
        havingValue = (AnnotationUtil.getBooleanAttributeValue(annotation, HAVING_VALUE) ?: true).toString(),
        matchIfMissing = AnnotationUtil.getBooleanAttributeValue(annotation, MATCH_IF_MISSING) ?: false
    )

    private fun booleanConstant(value: PsiAnnotationMemberValue): Boolean? {
        val expression = value as? PsiExpression ?: return null
        return JavaPsiFacade.getInstance(value.project).constantEvaluationHelper
            .computeConstantExpression(expression) as? Boolean
    }

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
        val PLATFORM_ANNOTATION_PACKAGES = listOf("java.lang.annotation.", "kotlin.annotation.", "kotlin.jvm.")
    }
}
