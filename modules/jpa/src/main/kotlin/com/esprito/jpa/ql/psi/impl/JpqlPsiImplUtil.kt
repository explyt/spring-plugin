package com.esprito.jpa.ql.psi.impl

import com.esprito.jpa.ql.psi.JpqlElementType
import com.esprito.jpa.ql.psi.JpqlMapBasedReferenceExpression

object JpqlPsiImplUtil {
    @JvmStatic
    fun getMapOperationType(expression: JpqlMapBasedReferenceExpression): JpqlElementType {
        return expression.node.firstChildNode.elementType as JpqlElementType
    }
}