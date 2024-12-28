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
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiPackageReference
import com.intellij.ui.SimpleTextAttributes

abstract class SpringConfigurationAnnotator : Annotator {

    protected fun annotateKey(element: PsiElement, holder: AnnotationHolder) {
        val offset = element.node.startOffset
        val annotatedOffsets = mutableSetOf<Int>()

        element.references.forEach { reference ->
            val textAttribute = when {
                reference is JavaClassReference ||
                        (reference is PsiPackageReference && reference.resolve() != null) ->
                    SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES

                else -> null
            }

            if (textAttribute == null) return@forEach
            val textRange = reference.rangeInElement.shiftRight(offset)
            if (!textRange.isEmpty && annotatedOffsets.add(textRange.startOffset)) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(textRange)
                    .enforcedTextAttributes(textAttribute.toTextAttributes())
                    .create()
            }
        }
    }

    protected fun annotateValue(element: PsiElement, holder: AnnotationHolder) {
        val offset = element.node.startOffset
        val references = element.references

        val annotatedOffsets = mutableSetOf<Int>()
        references.asSequence().forEach { reference ->
            val textAttribute = when (reference) {
                is ValueHintReference -> convertTypeToTextAttribute(reference.getResultType())
                is FileReference -> DefaultLanguageHighlighterColors.HIGHLIGHTED_REFERENCE
                is ProfilePsiReference -> reference.getTextAttributesKey()
                is ExplytPropertyReference -> reference.getTextAttributesKey()
                else -> null
            }

            if (textAttribute == null) return@forEach
            var textRange = reference.rangeInElement.shiftRight(offset)
            if (textRange.startOffset == 687) {
                textRange = TextRange(textRange.startOffset, 710)
            }
            if (!textRange.isEmpty && annotatedOffsets.add(textRange.startOffset)) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(textRange)
                    .textAttributes(textAttribute)
                    .create()
            }
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
}