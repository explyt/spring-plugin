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

package com.explyt.spring.core.intentions

import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.properties.dataRetriever.ConfigurationPropertyDataRetriever
import com.explyt.spring.core.properties.dataRetriever.ConfigurationPropertyDataRetrieverFactory
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.*
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.lombok.utils.decapitalize
import org.jetbrains.uast.*

class CreateConfigurationMetaDescriptionIntention : BaseCreateMetaDescriptionIntention() {

    override fun isAvailable(psiElement: PsiElement): Boolean {
        val uParent = getUParentForIdentifier(psiElement) ?: return false
        val dataRetriever = ConfigurationPropertyDataRetrieverFactory.createFor(uParent) ?: return false
        val psiClass = dataRetriever.getContainingClass() ?: return false
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return false

        val prefixValue = ConfigurationPropertyDataRetriever.getPrefixValue(psiClass)
        val memberName = dataRetriever.getMemberName() ?: return false
        if (prefixValue.isBlank()) return false

        val keyName = prefixValue + memberName.decapitalize()
        return SpringConfigurationPropertiesSearch.getInstance(psiElement.project)
            .findHint(module, keyName) == null
    }

    override fun isAvailable(file: PsiFile): Boolean {
        return UastLanguagePlugin.byLanguage(file.language) != null
    }

    override fun rootArrayName(): String = "hints"

    override fun createKeyValue(generator: JsonElementGenerator, propertyInfo: PropertyInfo): JsonObject {
        return generator.createValue(
            """
            {
              "name": "${propertyInfo.name}",
              "values": [],
              "providers": []
            }
            """.trimIndent()
        )
    }

    override fun getPropertyInfo(editor: Editor, file: PsiFile): PropertyInfo? {
        val dataRetriever = getPropertyDataRetriever(editor, file) ?: return null
        val psiClass = dataRetriever.getContainingClass() ?: return null
        val prefixValue = ConfigurationPropertyDataRetriever.getPrefixValue(psiClass)
        val memberName = dataRetriever.getMemberName() ?: return null
        val nameElement = dataRetriever.getNameElementPsi() ?: return null

        return PropertyInfo(
            name = prefixValue + memberName.decapitalize(),
            type = getType(nameElement),
            nameElement
        )
    }

    private fun getType(element: PsiElement): String {
        val uVariable = element.getUastParentOfType<UVariable>()
        val uParameter = element.getUastParentOfType<UParameter>()
        val uMethod = element.getUastParentOfType<UMethod>()

        val type = when {
            uVariable != null -> {
                val psiVariable = uVariable.javaPsi as? PsiVariable
                psiVariable?.type
            }

            uParameter != null -> {
                val psiParameter = uParameter.javaPsi as? PsiParameter
                psiParameter?.type
            }

            uMethod != null -> {
                val psiParameter = uMethod
                    .uastParameters
                    .firstOrNull()
                    ?.javaPsi as? PsiParameter
                psiParameter?.type
            }

            else -> null
        }

        return PsiTypesUtil.getPsiClass(type)?.qualifiedName ?: CommonClassNames.JAVA_LANG_STRING
    }

    private fun getPropertyDataRetriever(editor: Editor, file: PsiFile): ConfigurationPropertyDataRetriever? {
        val elementAtCaret = findElementAtCaret(editor, file) ?: return null
        val uParent = getUParentForIdentifier(elementAtCaret) ?: return null
        return ConfigurationPropertyDataRetrieverFactory.createFor(uParent)
    }

}