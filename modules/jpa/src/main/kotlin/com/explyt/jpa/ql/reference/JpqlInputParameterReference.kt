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

import com.explyt.jpa.ql.psi.JpqlInputParameterExpression
import com.explyt.jpa.ql.psi.impl.JpqlElementFactory
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

class JpqlInputParameterReference(
    expression: JpqlInputParameterExpression
) :
    PsiPolyVariantReferenceBase<JpqlInputParameterExpression>(
        expression,
        TextRange(0, expression.textLength)
    ) {

    override fun getVariants(): Array<Any> {
        return InputParameterReferenceResolver.EP
            .getExtensions(element.project)
            .flatMap {
                it.getVariants(element)
            }.toTypedArray()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        if (element.text.startsWith('?'))
            return element

        val newInputParameterExpression = JpqlElementFactory.getInstance(element.project)
            .createNamedInputParameter(newElementName)

        return element.replace(newInputParameterExpression)
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return InputParameterReferenceResolver.EP
            .getExtensions(element.project)
            .flatMap {
                it.resolve(element)
            }.toTypedArray()
    }
}