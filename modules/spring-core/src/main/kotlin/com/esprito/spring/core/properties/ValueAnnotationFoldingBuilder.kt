package com.esprito.spring.core.properties

import com.esprito.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.esprito.spring.core.completion.properties.DefinedConfigurationProperty
import com.esprito.spring.core.util.PropertyUtil
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.util.PsiLiteralUtil


class ValueAnnotationFoldingBuilder : FoldingBuilderEx() {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val module = ModuleUtilCore.findModuleForPsiElement(root) ?: return emptyArray()

        val group = FoldingGroup.newGroup("PropertyValue")
        val descriptors = mutableListOf<FoldingDescriptor>()

        root.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitLiteralExpression(literalExpression: PsiLiteralExpression) {
                super.visitLiteralExpression(literalExpression)

                val value = PsiLiteralUtil.getStringLiteralContent(literalExpression) ?: return
                val matchResult = PropertyUtil.VALUE_REGEX.matchEntire(value) ?: return

                val (key, defaultValue) = matchResult.destructured
                val propertyInfo = getPropertyInfo(module, key)

                if (propertyInfo != null || defaultValue.isNotEmpty()) {
                    descriptors.add(
                        FoldingDescriptor(
                            literalExpression.node,
                            literalExpression.textRange,
                            group, setOfNotNull(propertyInfo?.psiElement)
                        )
                    )
                }
            }
        })

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String? {
        val psiLiteralExpression = node.psi as? PsiLiteralExpression ?: return null
        val module = ModuleUtilCore.findModuleForPsiElement(psiLiteralExpression) ?: return null
        val text = PsiLiteralUtil.getStringLiteralContent(psiLiteralExpression) ?: return null
        val matchResult = PropertyUtil.VALUE_REGEX.matchEntire(text) ?: return null

        val (key, defaultValue) = matchResult.destructured

        val propertyValue = getPropertyInfo(module, key)
            ?: return defaultValue.ifBlank { StringUtil.THREE_DOTS }

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