package com.esprito.jpql.psi.impl

import com.esprito.jpql.psi.JpqlElementType
import com.esprito.jpql.psi.JpqlMapBasedReferenceExpression

object JpqlPsiImplUtil {
    @JvmStatic
    fun getMapOperationType(expression: JpqlMapBasedReferenceExpression): JpqlElementType {
        return expression.node.firstChildNode.elementType as JpqlElementType
    }
}