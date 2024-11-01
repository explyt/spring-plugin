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

package com.explyt.spring.core.inspections.quickfix

import com.explyt.spring.core.SpringCoreBundle
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager

class AddJsonElementQuickFix(element: PsiElement, val parameters: List<String>) : LocalQuickFixOnPsiElement(element) {
    override fun getFamilyName(): String =
        SpringCoreBundle.message("explyt.spring.inspection.metadata.config.quickfix")

    override fun getText(): String = familyName

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        val jsonObject = when (startElement) {
            is JsonProperty -> startElement.value as? JsonObject
            is JsonObject -> startElement
            else -> null
        }
        if (jsonObject == null) {
            return
        }

        val additionalJson = file as? JsonFile ?: return
        val containingFile = startElement.containingFile

        WriteCommandAction.runWriteCommandAction(project, "", null, {
            val generator = JsonElementGenerator(project)
            parameters.forEach {
                val hasProperty = jsonObject.propertyList.isNotEmpty()
                if (hasProperty) {
                    jsonObject.addBefore(generator.createComma(), jsonObject.lastChild)
                }
                val property = generator.createProperty(it, "\"\"")
                val added = jsonObject.addBefore(property, jsonObject.lastChild) as JsonProperty

                CodeStyleManager.getInstance(project)
                    .reformatText(additionalJson, 0, additionalJson.textLength)
                added.navigate(true)
            }
        }, containingFile)
    }

}