/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.MergedAnnotationAttributes
import com.explyt.spring.core.service.MergedAnnotationAttributes.Source
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.util.ExplytAnnotationUtil.getStringMemberValues
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.uast.evaluateString

object BeanAnnotationNames {
    private const val VALUE = "value"
    private const val NAME = "name"
    private val BEAN_NAME_ATTRIBUTES = setOf(VALUE, NAME)

    fun of(owner: PsiModifierListOwner): Set<String>? {
        val beanAnnotation = owner.annotations.firstOrNull { isBeanOrMetaAnnotatedByBean(it) } ?: return null
        val names = if (beanAnnotation.qualifiedName == SpringCoreClasses.BEAN) {
            declaredNames(beanAnnotation)
        } else {
            composedNames(owner, beanAnnotation)
        }
        return names?.ifEmpty { null }
    }

    private fun composedNames(owner: PsiModifierListOwner, beanAnnotation: PsiAnnotation): Set<String>? {
        val attributes = MergedAnnotationAttributes(
            SpringCoreClasses.BEAN,
            ::isMetaAnnotatedByBean,
            lazy { MergedAnnotationAttributes.conventionMappingApplies(MergedAnnotationAttributes.moduleOf(owner)) },
        )
        val sources = attributes.resolve(beanAnnotation, BEAN_NAME_ATTRIBUTES) ?: return null
        return sources.flatMap { namesOf(it) }.toNames()
    }

    private fun namesOf(source: Source): List<String> = when (source) {
        is Source.Declared -> source.annotation.getStringMemberValues(source.attribute).toList()
        is Source.Default -> MetaAnnotationsHolder.getValues(MergedAnnotationAttributes.defaultValueOf(source.attribute))
            .mapNotNull { it.evaluateString() }
    }

    private fun isBeanOrMetaAnnotatedByBean(annotation: PsiAnnotation): Boolean {
        if (annotation.qualifiedName == SpringCoreClasses.BEAN) return true
        if (MergedAnnotationAttributes.isLanguageAnnotation(annotation)) return false
        return MergedAnnotationAttributes.annotationTypeOf(annotation)?.let(::isMetaAnnotatedByBean) == true
    }

    private fun isMetaAnnotatedByBean(type: PsiClass): Boolean = type.isMetaAnnotatedBy(SpringCoreClasses.BEAN)

    private fun declaredNames(bean: PsiAnnotation): Set<String> =
        BEAN_NAME_ATTRIBUTES.flatMap { bean.getStringMemberValues(it) }.toNames()

    private fun List<String>.toNames(): Set<String> = filter(String::isNotBlank).toCollection(LinkedHashSet())
}
