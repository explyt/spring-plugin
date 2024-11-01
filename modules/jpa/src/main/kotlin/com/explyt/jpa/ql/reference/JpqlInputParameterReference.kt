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