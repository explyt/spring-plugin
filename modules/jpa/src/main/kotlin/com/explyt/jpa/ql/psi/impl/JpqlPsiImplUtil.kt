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

package com.explyt.jpa.ql.psi.impl

import com.explyt.jpa.ql.psi.*
import com.explyt.jpa.ql.reference.JpqlFullyQualifiedElementReference
import com.explyt.jpa.ql.reference.JpqlInputParameterReference
import com.explyt.jpa.ql.reference.JpqlReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
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

        val jpqlElementFactory = JpqlElementFactory.getInstance(element.project)

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
        val jpqlElementFactory = JpqlElementFactory.getInstance(element.project)

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
        if (element.parent is JpqlFullyQualifiedConstructor) {
            return JpqlFullyQualifiedElementReference(element)
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

    @JvmStatic
    fun getValue(element: JpqlBooleanLiteral): Boolean {
        return element.text.equals("true", ignoreCase = true)
    }

    @JvmStatic
    fun getType(element: JpqlExpression): JpqlType {
        return JpqlTypeResolver.getInstance(element.project).resolveType(element)
    }

    @JvmStatic
    fun getOperator(element: JpqlComparisonExpression): JpqlTokenType {
        return element.children.first {
            JpqlTokensSets.OPERATORS.contains(it.elementType)
        }.elementType as JpqlTokenType
    }
}
