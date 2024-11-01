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

package com.explyt.spring.web.references

import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.getUastParentOfType

class ExplytPathVariableReference(element: PsiElement, private val methodName: String, rangeInElement: TextRange) :
    PsiReferenceBase<PsiElement>(element, rangeInElement), PsiReference, HighlightedReference {

    override fun resolve(): PsiElement? {
        val method = getAnnotatedMethod() ?: return null
        return SpringWebUtil.collectPathVariables(method)
            .firstOrNull {
                it.name == methodName
            }
            ?.psiElement
    }

    override fun getVariants(): Array<Any> {
        val method = getAnnotatedMethod() ?: return emptyArray()
        return SpringWebUtil.collectPathVariables(method)
            .mapTo(mutableListOf()) {
                LookupElementBuilder.create(it.name)
            }.toTypedArray()

    }

    private fun getAnnotatedMethod(): PsiMethod? {
        val uastParent = element
            .getUastParentOfType<UAnnotation>()
            ?.uastParent ?: return null

        return uastParent.javaPsi as? PsiMethod
    }

}