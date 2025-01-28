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

package com.explyt.spring.core.util

import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.intellij.lang.properties.IProperty
import com.intellij.lang.properties.psi.PropertiesElementFactory
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue

object RenameUtil {

    fun renameSameProperty(project: Project, baseElement: PsiElement, oldKey: String, newKey: String) {
        val module = ModuleUtilCore.findModuleForPsiElement(baseElement) ?: return

        val properties = DefinedConfigurationPropertiesSearch.getInstance(project)
            .findProperties(module, oldKey)

        val oldKeyParts = oldKey.split(".")
        val newKeyParts = newKey.split(".")

        properties
            .mapNotNull { it.psiElement }
            .filter { it.containingFile != baseElement.containingFile }
            .forEach { elementToRename ->
                when (elementToRename) {
                    is YAMLKeyValue -> {
                        val fullName = YAMLUtil.getConfigFullName(elementToRename)
                        if (fullName == oldKey && oldKeyParts.size == newKeyParts.size) {
                            renameYamlKeyValue(project, elementToRename, newKeyParts)
                        }
                    }

                    is IProperty -> renameIProperty(project, elementToRename, newKey)
                }
            }
    }

    private fun renameYamlKeyValue(project: Project, elementToRename: YAMLKeyValue, newKeyParts: List<String>) {
        val key = elementToRename.key ?: return
        val newKey = newKeyParts.last()

        if (key.text != newKey) {

            val newElement = YAMLElementGenerator.getInstance(project)
                .createYamlKeyValue(newKey, elementToRename.valueText)

            WriteCommandAction.runWriteCommandAction(project) {
                if (!elementToRename.isValid) return@runWriteCommandAction

                if (key.isValid && newElement.key != null) {
                    key.replace(newElement.key!!)
                }
            }
        }

        val parentElement = elementToRename.parentOfType<YAMLKeyValue>() ?: return
        renameYamlKeyValue(project, parentElement, newKeyParts.dropLast(1))
    }

    private fun renameIProperty(project: Project, elementToRename: PsiElement, newKey: String) {
        if (elementToRename !is IProperty) return
        val value = elementToRename.value ?: return
        if (value == newKey) return
        val newElement = PropertiesElementFactory
            .createProperty(project, newKey, value, null) as? PsiElement ?: return
        WriteCommandAction.runWriteCommandAction(project) {
            elementToRename.replace(newElement)
        }
    }

}