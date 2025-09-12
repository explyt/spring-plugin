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

import com.cronutils.descriptor.CronDescriptor
import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.completion.properties.DefinedConfigurationProperty
import com.explyt.spring.core.inspections.CRON_PARSER
import com.explyt.spring.core.inspections.SpringScheduledInspection
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.util.PsiLiteralUtil
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastVisitor
import java.util.*


val CRON_DESCRIPTOR: CronDescriptor = CronDescriptor.instance(Locale.UK)

class ValueAnnotationFoldingBuilder : FoldingBuilderEx() {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val module = ModuleUtilCore.findModuleForPsiElement(root) ?: return emptyArray()
        if (!SpringCoreUtil.isSpringModule(module)) return emptyArray()
        val descriptors = mutableListOf<FoldingDescriptor>()
        val uRoot = root.toUElement() ?: return emptyArray()

        uRoot.accept(object : AbstractUastVisitor() {
            override fun visitExpression(node: UExpression): Boolean {
                processUElement(node, module, descriptors)
                return super.visitExpression(node)
            }
        })
        return descriptors.toTypedArray()
    }

    private fun processUElement(
        uElement: UExpression, module: Module, descriptors: MutableList<FoldingDescriptor>
    ) {
        if (uElement is UPolyadicExpression || uElement is ULiteralExpression) {
            val value = try {
                uElement.evaluate() as? String
            } catch (_: Exception) {
                null
            } ?: return
            val element = uElement.sourcePsi ?: return
            val matchResult = PropertyUtil.VALUE_REGEX.matchEntire(value)
            if (matchResult == null) {
                val uastParent = uElement.uastParent as? UNamedExpression ?: return
                if (uastParent.name != SpringScheduledInspection.CRON_PARAM) return
                val cronValue = uElement.evaluate() as? String ?: return
                val placeholder = parseCron(cronValue, true) ?: return
                descriptors.add(
                    FoldingDescriptor(element.node, element.textRange, group, placeholder)
                )
                return
            }

            val (key, defaultValue) = matchResult.destructured
            val propertyInfo = getPropertyInfo(module, key)

            if (propertyInfo != null || defaultValue.isNotEmpty()) {
                val placeholder = getPlaceholder(uElement, propertyInfo, defaultValue) ?: return
                descriptors.add(
                    FoldingDescriptor(element.node, element.textRange, group, placeholder)
                )
            }
        }
    }

    private fun getPlaceholder(
        uElement: UExpression, propertyInfo: DefinedConfigurationProperty?, defaultValue: String
    ): String? {
        val placeholder = propertyInfo?.value ?: defaultValue
        val uastParent = uElement.uastParent
        if (uastParent is UNamedExpression && uastParent.name == SpringScheduledInspection.CRON_PARAM) {
            return parseCron(placeholder)
        }
        return placeholder
    }

    private fun parseCron(placeholder: String, returnNullOnError: Boolean = false): String? {
        return try {
            val description = CRON_DESCRIPTOR.describe(CRON_PARSER.parse(placeholder))
            "$placeholder :$description"
        } catch (e: Exception) {
            if (returnNullOnError) null else placeholder
        }
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

val group: FoldingGroup = FoldingGroup.newGroup("PropertyValue")