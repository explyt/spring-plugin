package com.esprito.spring.core.properties

import com.esprito.spring.core.SpringCoreClasses.PROPERTY_RESOLVER
import com.esprito.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.esprito.spring.core.completion.properties.DefinedConfigurationProperty
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
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.sourcePsiElement
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastVisitor


class GetPropertyMethodFoldingBuilder : FoldingBuilderEx() {
    private val propertyMethodNames = setOf("getProperty", "containsProperty", "getRequiredProperty")

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val module = ModuleUtilCore.findModuleForPsiElement(root) ?: return emptyArray()

        val group = FoldingGroup.newGroup("PropertyValue")
        val descriptors = mutableListOf<FoldingDescriptor>()
        val uRoot = root.toUElement() ?: return emptyArray()

        uRoot.accept(object : AbstractUastVisitor() {
            override fun visitLiteralExpression(node: ULiteralExpression): Boolean {
                val callExpression = node.uastParent as? UCallExpression ?: return false
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
        })

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String? {
        val module = ModuleUtilCore.findModuleForPsiElement(node.psi) ?: return null

        val key = ElementManipulators.getValueText(node.psi)
        if (key.isBlank()) return null

        val propertyValue = getPropertyInfo(module, key) ?: return null

        return propertyValue.value
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