package com.esprito.jpa.ql.psi.impl

import com.esprito.jpa.ql.psi.*
import com.esprito.jpa.ql.reference.JpqlInputParameterReference
import com.esprito.jpa.ql.reference.JpqlReference
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType


object JpqlPsiImplUtil {
    @JvmStatic
    fun getMapOperationType(expression: JpqlMapBasedReferenceExpression): JpqlElementType {
        return expression.node.firstChildNode.elementType as JpqlElementType
    }

    @JvmStatic
    fun getName(element: JpqlAliasDeclaration): String {
        return PsiTreeUtil.lastChild(element).text
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
    fun getName(element: JpqlIdentifier): String {
        return element.text
    }

    @JvmStatic
    fun setName(element: JpqlIdentifier, newName: String): PsiElement {
        val jpqlElementFactory = element.project.service<JpqlElementFactory>()

        val newIdentifier = jpqlElementFactory.createIdentifier(newName)

        return element.replace(newIdentifier)
    }

    @JvmStatic
    fun getNameIdentifier(element: JpqlAliasDeclaration): PsiElement? {
        return PsiTreeUtil.findChildOfType(element, JpqlIdentifier::class.java)
    }

    @JvmStatic
    fun getReference(element: JpqlIdentifier): PsiPolyVariantReference? {
        val isAliasIdentifier = element.parentOfType<JpqlAliasDeclaration>() != null
        if (isAliasIdentifier) {
            return null
        }

        return JpqlReference(element)
    }

    @JvmStatic
    fun getReferencedElement(element: JpqlAliasDeclaration): PsiElement? {
        val parent = element.parent
        if (parent is JpqlCollectionMemberDeclaration) {
            return parent.referenceExpression
        }

        return PsiTreeUtil.skipSiblingsBackward(element, PsiWhiteSpace::class.java)
    }

    @JvmStatic
    fun getReference(element: JpqlInputParameterExpression): PsiPolyVariantReference {
        return JpqlInputParameterReference(element)
    }
}
