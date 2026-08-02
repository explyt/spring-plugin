/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.quickfix

import com.explyt.spring.core.SpringCoreBundle.message
import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import org.jetbrains.kotlin.idea.base.codeInsight.ShortenReferencesFacility
import org.jetbrains.kotlin.idea.base.psi.replaced
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.uast.UAnnotation

/**
 * Quick-fix that replaces the highlighted annotation with [newFqn].
 *
 * Unlike [RewriteAnnotationQuickFix], it works on the annotation **source** PSI ([PsiAnnotation] for Java,
 * [KtAnnotationEntry] for Kotlin) instead of the UAST `javaPsi` owner. For Kotlin that owner is light PSI, which the
 * platform add-annotation machinery rejects, leaving the reported problem without any usable fix.
 *
 * Declared attributes are carried over: [attributeRenames] maps an attribute name of the old annotation to its
 * counterpart on the new one (identity entries included). Values are copied verbatim; a Kotlin positional argument is
 * wrapped into a collection literal when the new attribute is an array. Call [unmappedAttributes] before offering the
 * fix - an attribute outside [attributeRenames] cannot be migrated without changing semantics.
 */
class ReplaceAnnotationQuickFix(
    private val newFqn: String,
    @FileModifier.SafeFieldForPreview
    private val attributeRenames: Map<String, String> = emptyMap()
) : LocalQuickFix {

    override fun getName(): String =
        message("explyt.spring.inspection.replace.annotation.fix", newFqn.substringAfterLast('.'))

    override fun getFamilyName(): String = message("explyt.spring.inspection.replace.annotation.fix.family")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        when (val element = descriptor.psiElement) {
            is KtAnnotationEntry -> replaceKotlinAnnotation(project, element)
            is PsiAnnotation -> replaceJavaAnnotation(project, element)
        }
    }

    private fun replaceJavaAnnotation(project: Project, annotation: PsiAnnotation) {
        val arguments = annotation.parameterList.attributes.map { pair ->
            val oldName = pair.name ?: DEFAULT_ATTRIBUTE
            val newName = attributeRenames[oldName] ?: oldName
            val valueText = pair.value?.text ?: return@map pair.text
            // Keep an implicit `value` implicit; Java accepts an array initializer for any array attribute as is.
            if (pair.name == null && newName == DEFAULT_ATTRIBUTE) valueText else "$newName = $valueText"
        }
        val newAnnotation = JavaPsiFacade.getElementFactory(project)
            .createAnnotationFromText(annotationText(arguments), annotation)
        val replaced = annotation.replace(newAnnotation)
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(replaced)
    }

    private fun replaceKotlinAnnotation(project: Project, entry: KtAnnotationEntry) {
        val newAnnotationClass = JavaPsiFacade.getInstance(project).findClass(newFqn, entry.resolveScope)

        val positional = mutableListOf<String>()
        val named = mutableListOf<String>()
        for (argument in entry.valueArguments) {
            val expressionText = argument.getArgumentExpression()?.text ?: return
            val oldName = argument.getArgumentName()?.asName?.asString()
            if (oldName == null) {
                positional += expressionText
                continue
            }
            val newName = attributeRenames[oldName] ?: oldName
            named += "$newName = ${adaptValue(newAnnotationClass, newName, listOf(expressionText))}"
        }

        val renamedDefault = attributeRenames[DEFAULT_ATTRIBUTE]?.takeIf { it != DEFAULT_ATTRIBUTE }
        val arguments = when {
            positional.isEmpty() -> named
            // The positional argument(s) keep their name: leave them positional.
            renamedDefault == null -> positional + named
            else -> listOf("$renamedDefault = ${adaptValue(newAnnotationClass, renamedDefault, positional)}") + named
        }

        // `@field:MockBean` and friends must keep their use-site target.
        val useSiteTarget = entry.useSiteTarget?.text?.removeSuffix(":")?.let { "$it:" }.orEmpty()
        val newEntry = KtPsiFactory(project).createAnnotationEntry(annotationText(arguments, useSiteTarget))
        ShortenReferencesFacility.getInstance().shorten(entry.replaced(newEntry))
    }

    private fun annotationText(arguments: List<String>, useSiteTarget: String = ""): String {
        val prefix = "@$useSiteTarget$newFqn"
        return if (arguments.isEmpty()) prefix else arguments.joinToString(", ", "$prefix(", ")")
    }

    /**
     * Renders [valueTexts] for the Kotlin attribute [attributeName]: an array attribute needs a collection literal,
     * so a bare (or vararg-style) value has to be wrapped, while an already-collection value is kept as is.
     */
    private fun adaptValue(annotationClass: PsiClass?, attributeName: String, valueTexts: List<String>): String {
        val single = valueTexts.singleOrNull()
        if (!isArrayAttribute(annotationClass, attributeName)) return single ?: valueTexts.joinToString(", ")
        if (single != null && (single.startsWith("[") || single.startsWith("arrayOf("))) return single
        return valueTexts.joinToString(", ", "[", "]")
    }

    private fun isArrayAttribute(annotationClass: PsiClass?, attributeName: String): Boolean {
        val attribute = annotationClass?.findMethodsByName(attributeName, false)?.firstOrNull() ?: return false
        return attribute.returnType is PsiArrayType
    }

    companion object {
        private const val DEFAULT_ATTRIBUTE = PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME

        /**
         * Names of attributes declared on [annotation] that cannot be carried over to the replacement, i.e. that
         * would make the rewrite drop or corrupt user configuration. Empty when the fix is safe to offer.
         *
         * An attribute blocks the rewrite when it has no counterpart in [attributeRenames], or when two declared
         * attributes map onto the same replacement attribute (which would emit it twice).
         */
        fun unmappedAttributes(annotation: UAnnotation, attributeRenames: Map<String, String>): List<String> {
            val declaredNames = when (val sourcePsi = annotation.sourcePsi) {
                is PsiAnnotation -> sourcePsi.parameterList.attributes.map { it.name ?: DEFAULT_ATTRIBUTE }
                is KtAnnotationEntry -> sourcePsi.valueArguments
                    .map { it.getArgumentName()?.asName?.asString() ?: DEFAULT_ATTRIBUTE }
                // Unknown language: the fix would not be able to rewrite it either.
                else -> return listOf(DEFAULT_ATTRIBUTE)
            }
            val (mapped, unmapped) = declaredNames.partition { it in attributeRenames }
            val colliding = mapped
                .groupBy { attributeRenames.getValue(it) }
                .filterValues { it.size > 1 }
                .values.flatten()
            return unmapped + colliding
        }
    }
}
