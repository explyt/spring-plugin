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

package com.explyt.spring.core.providers

import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.references.PrefixReference
import com.explyt.spring.core.references.PrefixReferenceType
import com.explyt.spring.core.util.PropertyUtil
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.UExpression

class FileReferenceProvider(private val possibleFileTypes: Array<FileType> = emptyArray()) :
    UastInjectionHostReferenceProvider() {
    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext
    ): Array<PsiReference> {
        val text = ElementManipulators.getValueText(host)
        val psiElement = uExpression.sourcePsi ?: return PsiReference.EMPTY_ARRAY
        val textRange = ElementManipulators.getValueTextRange(psiElement)

        val references = mutableListOf<PsiReference>()
        when {
            text.startsWith(SpringProperties.PREFIX_FILE) -> {
                references += PropertyUtil.getReferenceByFilePrefix(
                    text, psiElement, textRange, possibleFileTypes, null
                )
            }

            text.startsWith(SpringProperties.PREFIX_CLASSPATH) -> {
                references += PropertyUtil.getReferenceByClasspathPrefix(
                    text, SpringProperties.PREFIX_CLASSPATH, psiElement, textRange, possibleFileTypes, null
                )
            }

            text.startsWith(SpringProperties.PREFIX_CLASSPATH_STAR) -> {
                references += PropertyUtil.getReferenceByClasspathPrefix(
                    text, SpringProperties.PREFIX_CLASSPATH_STAR, psiElement, textRange, possibleFileTypes, null
                )
            }

            else -> {
                references += PropertyUtil.getReferenceWithoutPrefix(
                    text, psiElement, textRange, possibleFileTypes, null
                )
                references += PrefixReference(psiElement, textRange, PrefixReferenceType.FILE_PROPERTY)
            }
        }
        return references.toTypedArray()
    }
}