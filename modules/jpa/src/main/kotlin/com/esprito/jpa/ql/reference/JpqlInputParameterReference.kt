package com.esprito.jpa.ql.reference

import com.esprito.jpa.ql.psi.JpqlInputParameterExpression
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

class JpqlInputParameterReference(
    expression: JpqlInputParameterExpression
) :
    PsiPolyVariantReferenceBase<JpqlInputParameterExpression>(
        expression,
        TextRange(0, expression.textLength)
    ) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return InputParameterReferenceResolver.EP
            .getExtensions(element.project)
            .flatMap {
                it.resolve(element)
            }.toTypedArray()
    }
}