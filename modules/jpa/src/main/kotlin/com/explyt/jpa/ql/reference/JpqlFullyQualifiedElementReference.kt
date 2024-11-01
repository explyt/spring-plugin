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

package com.explyt.jpa.ql.reference

import com.explyt.jpa.ql.psi.JpqlFullyQualifiedConstructor
import com.explyt.jpa.ql.psi.JpqlIdentifier
import com.explyt.jpa.ql.psi.impl.JpqlElementFactory
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType

class JpqlFullyQualifiedElementReference(
    identifier: JpqlIdentifier
) :
    PsiPolyVariantReferenceBase<JpqlIdentifier>(
        identifier,
        TextRange(0, identifier.textLength)
    ) {
    private val referenceProvider = JavaClassReferenceProvider()

    override fun resolve(): PsiElement? {
        val resolveResults = multiResolve(false)
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

    override fun getVariants(): Array<Any> {
        return getElementReference()?.variants ?: return emptyArray()
    }

    override fun multiResolve(incompleteCode: Boolean): Array<PsiElementResolveResult> {
        return getElementReference()?.let {
            it.resolve()?.let { referencedElement ->
                arrayOf(PsiElementResolveResult(referencedElement))
            }
        } ?: emptyArray()
    }

    private fun getElementReference(): PsiReference? {
        val fqConstructor = element.parentOfType<JpqlFullyQualifiedConstructor>() ?: return null

        var currentElement: JpqlIdentifier? = element
        var countToSkip = -1
        while (currentElement != null) {
            currentElement = PsiTreeUtil.getPrevSiblingOfType(currentElement, JpqlIdentifier::class.java)
            countToSkip++
        }

        return JavaClassReferenceSet(
            fqConstructor.text,
            fqConstructor,
            0,
            false,
            referenceProvider
        ).references.asSequence()
            .drop(countToSkip)
            .firstOrNull()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val newConstructorElement = JpqlElementFactory.getInstance(element.project)
            .createIdentifier(newElementName)

        return element.replace(newConstructorElement)
    }

}