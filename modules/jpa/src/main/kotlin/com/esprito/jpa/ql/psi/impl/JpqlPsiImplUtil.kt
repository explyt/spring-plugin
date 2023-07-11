package com.esprito.jpa.ql.psi.impl

import com.esprito.jpa.ql.psi.*
import com.esprito.jpa.ql.reference.JpqlReference
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.parentOfType


object JpqlPsiImplUtil {
    @JvmStatic
    fun getMapOperationType(expression: JpqlMapBasedReferenceExpression): JpqlElementType {
        return expression.node.firstChildNode.elementType as JpqlElementType
    }

    @JvmStatic
    fun getName(element: JpqlAliasDeclaration): String {
        return element.text
    }

    @JvmStatic
    fun setName(element: JpqlAliasDeclaration, newName: String): PsiElement {
        val oldIdentifier = element.node.findChildByType(JpqlTypes.IDENTIFIER)
            ?: return element

        val jpqlElementFactory = element.project.service<JpqlElementFactory>()

        val newIdentifier = jpqlElementFactory.createIdentifier(newName)

        element.node.replaceChild(oldIdentifier, newIdentifier.node)

        return element
    }

    @JvmStatic
    fun getReference(element: JpqlIdentifier): PsiPolyVariantReference? {
        return if(element.parentOfType<JpqlAliasDeclaration>() == null) {
            JpqlReference(element)
        } else {
            null
        }
    }
}