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

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiPackageReference
import com.intellij.ui.SimpleTextAttributes

abstract class SpringConfigurationAnnotator : Annotator {

    protected fun annotateKey(element: PsiElement, holder: AnnotationHolder) {
        val offset = element.node.startOffset
        val annotatedOffsets = mutableSetOf<Int>()

        element.references.forEach { reference ->
            val key = when {
                reference is JavaClassReference ||
                        (reference is PsiPackageReference && reference.resolve() != null) ->
                    SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES

                else -> null
            }

            if (key == null) return@forEach
            val textRange = reference.rangeInElement.shiftRight(offset)
            if (!textRange.isEmpty && annotatedOffsets.add(textRange.startOffset)) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(textRange)
                    .enforcedTextAttributes(key.toTextAttributes())
                    .create()
            }
        }
    }
}
