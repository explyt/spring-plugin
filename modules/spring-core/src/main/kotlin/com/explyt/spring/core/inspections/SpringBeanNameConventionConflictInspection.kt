/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.MergedAnnotationAttributes
import com.explyt.spring.core.service.MergedAnnotationAttributes.ConventionOverride
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.util.ExplytAnnotationUtil.getStringMemberValues
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.evaluateString

class SpringBeanNameConventionConflictInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val method = uMethod.javaPsi
        val composed = method.annotations.filter(::isComposedBean).ifEmpty { return null }
        val attributes = MergedAnnotationAttributes(
            SpringCoreClasses.BEAN,
            { it.isMetaAnnotatedBy(SpringCoreClasses.BEAN) },
            lazy { MergedAnnotationAttributes.conventionMappingApplies(MergedAnnotationAttributes.moduleOf(method)) },
        )
        return composed.mapNotNull { annotation ->
            val conflict = attributes.conventionOverrideOf(annotation, NAME)?.let(::conflictOf) ?: return@mapNotNull null
            val shortName = annotation.qualifiedName?.substringAfterLast('.') ?: return@mapNotNull null
            val anchor = uMethod.uAnnotations.firstOrNull { it.qualifiedName == annotation.qualifiedName }?.sourcePsi
                ?: return@mapNotNull null
            manager.createProblemDescriptor(
                anchor,
                SpringCoreBundle.message(
                    "explyt.spring.inspection.bean.name.convention.conflict",
                    shortName,
                    conflict.formatNames(),
                ),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            )
        }.toTypedArray()
    }

    private fun isComposedBean(annotation: PsiAnnotation): Boolean {
        if (annotation.qualifiedName == SpringCoreClasses.BEAN) return false
        if (MergedAnnotationAttributes.isLanguageAnnotation(annotation)) return false
        return MergedAnnotationAttributes.annotationTypeOf(annotation)?.isMetaAnnotatedBy(SpringCoreClasses.BEAN) == true
    }

    private fun List<String>.formatNames(): String = if (size == 1) first() else joinToString(", ", prefix = "{\"", postfix = "\"}")

    private fun conflictOf(override: ConventionOverride): List<String>? {
        if (override.useSite.attribute != NAME) return null
        if (override.root.findDeclaredAttributeValue(NAME) != null) return null
        val fixedNames = override.root.getStringMemberValues(VALUE).toList().ifEmpty { return null }
        val defaultNames = defaultNamesOf(override) ?: return null
        val useSiteNames = override.useSite.annotation.getStringMemberValues(NAME).toList()
        if (useSiteNames == fixedNames || useSiteNames == defaultNames) return null
        return fixedNames
    }

    private fun defaultNamesOf(override: ConventionOverride): List<String>? {
        val type = MergedAnnotationAttributes.annotationTypeOf(override.useSite.annotation) ?: return null
        val attribute: PsiMethod = type.findMethodsByName(NAME, false).firstOrNull() ?: return null
        return MetaAnnotationsHolder.getValues(MergedAnnotationAttributes.defaultValueOf(attribute))
            .map { it.evaluateString() }
            .takeIf { values -> values.none { it == null } }
            ?.filterNotNull()
    }

    private companion object {
        const val NAME = "name"
        const val VALUE = "value"
    }
}
