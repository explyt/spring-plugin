/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.quickfix

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.utils.ExplytJsonUtil
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
            var toNavigate: JsonProperty? = null

            parameters.forEach { name ->
                val property = generator.createProperty(name, "\"\"")

                toNavigate = ExplytJsonUtil.addPropertyToObject(property, jsonObject, generator)
            }
            CodeStyleManager.getInstance(project)
                .reformatText(additionalJson, 0, additionalJson.textLength)
            toNavigate?.navigate(true)
        }, containingFile)
    }

}