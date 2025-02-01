/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.annotator

import com.explyt.spring.core.color.ExplytConfigurationHighlighting
import com.explyt.spring.core.language.profiles.ProfilePsiReference
import com.explyt.spring.core.properties.references.ExplytPropertyReference
import com.explyt.spring.core.properties.references.ValueHintReference
import com.explyt.spring.core.properties.references.ValueHintReference.ResultType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiPackageReference
import com.intellij.ui.SimpleTextAttributes

abstract class SpringConfigurationAnnotator : Annotator {

    protected fun annotateKey(element: PsiElement, holder: AnnotationHolder) {
        val offset = element.node.startOffset
        val annotatedOffsets = mutableSetOf<Int>()

        element.references.forEach { reference ->
            val textRange = reference.rangeInElement.shiftRight(offset)
            if (isValidTextRange(textRange, annotatedOffsets)) {
                val enforcedTextAttribute = getEnforcedTextAttributeForReferenceKey(reference)
                if (enforcedTextAttribute != null) {
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(textRange)
                        .enforcedTextAttributes(enforcedTextAttribute.toTextAttributes())
                        .create()
                }
            }
        }
    }

    private fun getEnforcedTextAttributeForReferenceKey(reference: PsiReference): SimpleTextAttributes? {
        return when {
            reference is JavaClassReference -> SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES
            reference is PsiPackageReference && reference.resolve() != null -> SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES
            else -> null
        }
    }

    protected fun annotateValue(element: PsiElement, holder: AnnotationHolder) {
        val offset = element.node.startOffset
        val references = element.references

        val annotatedOffsets = mutableSetOf<Int>()
        references.asSequence().forEach { reference ->
            val textAttribute = getTextAttributeForReferenceValue(reference) ?: return@forEach
            val textRange = reference.rangeInElement.shiftRight(offset)

            if (isValidTextRange(textRange, annotatedOffsets)) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(textRange)
                    .textAttributes(textAttribute)
                    .create()
            }
        }
    }

    private fun getTextAttributeForReferenceValue(reference: PsiReference?): TextAttributesKey? {
        return when (reference) {
            is ValueHintReference -> convertTypeToTextAttribute(reference.getResultType())
            is FileReference -> DefaultLanguageHighlighterColors.HIGHLIGHTED_REFERENCE
            is ProfilePsiReference -> reference.getTextAttributesKey()
            is ExplytPropertyReference -> reference.getTextAttributesKey() //            is YamlKeyMapValueReference -> DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
            else -> null
        }
    }

    private fun convertTypeToTextAttribute(type: ResultType?): TextAttributesKey? {
        return when (type) {
            ResultType.CLASS_REFERENCE -> DefaultLanguageHighlighterColors.CLASS_REFERENCE
            ResultType.SPRING_BEAN -> ExplytConfigurationHighlighting.BEAN_HIGHLIGHTER_KEY
            ResultType.ENUM -> DefaultLanguageHighlighterColors.STATIC_FIELD
            ResultType.METADATA -> DefaultLanguageHighlighterColors.METADATA
            else -> null
        }
    }

    private fun isValidTextRange(
        textRange: TextRange,
        annotatedOffsets: MutableSet<Int>,
    ): Boolean {
        return !textRange.isEmpty
                && annotatedOffsets.add(textRange.startOffset)
    }

}