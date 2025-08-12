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

package com.explyt.spring.core.properties

import com.explyt.spring.core.SpringCoreClasses.PROPERTY_RESOLVER
import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.completion.properties.DefinedConfigurationProperty
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.codeInspection.isInheritorOf
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastVisitor


class GetPropertyMethodFoldingBuilder : FoldingBuilderEx() {
    private val propertyMethodNames = setOf("getProperty", "containsProperty", "getRequiredProperty")

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        if (!SpringCoreUtil.isSpringProject(root.project)) return emptyArray()
        val module = ModuleUtilCore.findModuleForPsiElement(root) ?: return emptyArray()

        val group = FoldingGroup.newGroup("PropertyValue")
        val descriptors = mutableListOf<FoldingDescriptor>()
        val uRoot = root.toUElement() ?: return emptyArray()

        val visitor = object : AbstractUastVisitor() {
            override fun visitLiteralExpression(node: ULiteralExpression): Boolean {
                val callExpression = getUCallExpression(node) ?: return false
                if (callExpression.methodIdentifier?.name !in propertyMethodNames) return false
                if (callExpression.receiverType?.isInheritorOf(PROPERTY_RESOLVER)
                    != true
                ) return false

                val psiElement = node.sourcePsiElement ?: return false
                val key = ElementManipulators.getValueText(psiElement)
                if (key.isBlank()) return false

                val propertyInfo = getPropertyInfo(module, key)

                if (propertyInfo != null) {
                    descriptors.add(
                        FoldingDescriptor(
                            psiElement.node,
                            psiElement.textRange,
                            group, setOfNotNull(propertyInfo.psiElement)
                        )
                    )
                }
                return super.visitLiteralExpression(node)
            }

            private fun getUCallExpression(node: ULiteralExpression): UCallExpression? {
                val expression = node.uastParent as? UCallExpression
                    ?: (node.uastParent as? UPolyadicExpression)?.uastParent as? UCallExpression
                return expression
            }
        }
        try {
            uRoot.accept(visitor)
        } catch (_: Exception) {
        }

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String? {
        if (!SpringCoreUtil.isSpringProject(node.psi.project)) return null
        val key = ElementManipulators.getValueText(node.psi)
        if (key.isBlank()) return null
        val module = ModuleUtilCore.findModuleForPsiElement(node.psi) ?: return null
        return getPropertyInfo(module, key)?.value
    }

    private fun getPropertyInfo(
        module: Module,
        key: String
    ): DefinedConfigurationProperty? {
        return DefinedConfigurationPropertiesSearch.getInstance(module.project)
            .findProperties(module, key)
            .firstOrNull()
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = true

}