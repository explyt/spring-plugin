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

package com.explyt.spring.web.providers

import com.explyt.spring.web.references.ExplytControllerMethodReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UPolyadicExpression


class RedirectUrlControllerMethodReferenceProvider : UastInjectionHostReferenceProvider() {

    override fun getReferencesForInjectionHost(
        uExpression: UExpression, host: PsiLanguageInjectionHost, context: ProcessingContext
    ): Array<PsiReference> {
        val psiElement = uExpression.sourcePsi ?: return emptyArray()
        if (uExpression !is ULiteralExpression && uExpression !is UPolyadicExpression) return emptyArray()

        val text = ElementManipulators.getValueText(psiElement)
        val textRange = ElementManipulators.getValueTextRange(psiElement)

        for (prefix in listOf(REDIRECT, FORWARD)) {
            if (!text.startsWith(prefix)) continue

            return arrayOf(
                ExplytControllerMethodReference(
                    host,
                    text.removePrefix(prefix),
                    null,
                    TextRange(textRange.startOffset + prefix.length, textRange.endOffset)
                )
            )
        }

        return emptyArray()
    }

    companion object {
        private const val REDIRECT = "redirect:"
        private const val FORWARD = "forward:"
    }

}