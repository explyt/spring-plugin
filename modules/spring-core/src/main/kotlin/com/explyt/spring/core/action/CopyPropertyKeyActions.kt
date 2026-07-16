/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.action

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.util.ActionUtil
import com.explyt.spring.core.util.PropertyUtil
import com.intellij.lang.properties.PropertiesFileType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection
import org.jetbrains.yaml.YAMLFileType

/**
 * Base action that copies the canonical key of the property/YAML entry under the caret to the clipboard.
 * Concrete actions decide how the resolved key is transformed before copying.
 */
abstract class CopyPropertyKeyActionBase(text: String) : AnAction(text) {

    init {
        templatePresentation.icon = SpringIcons.Property
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        ActionUtil.isEnabledAndVisible(e, resolveFullKey(e) != null)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val fullKey = resolveFullKey(e) ?: return
        CopyPasteManager.getInstance().setContents(StringSelection(transform(fullKey)))
    }

    protected abstract fun transform(fullKey: String): String

    private fun resolveFullKey(e: AnActionEvent): String? {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return null
        if (virtualFile.fileType != PropertiesFileType.INSTANCE && virtualFile.fileType != YAMLFileType.YML) return null

        val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return null
        val offset = editor.caretModel.offset
        val element = psiFile.findElementAt(offset)
            ?: psiFile.findElementAt(offset - 1)
            ?: return null
        return PropertyUtil.getFullPropertyKey(element)?.takeIf { it.isNotBlank() }
    }
}

/**
 * Copies the full canonical property path (e.g. `spring.datasource.url`) of the key under the caret.
 */
class CopyPropertyPathAction :
    CopyPropertyKeyActionBase(SpringCoreBundle.message("explyt.spring.properties.action.copy.path")) {

    override fun transform(fullKey: String): String = fullKey
}

/**
 * Copies the environment variable name of the key under the caret following Spring Boot relaxed binding rules
 * (e.g. `spring.datasource.url` -> `SPRING_DATASOURCE_URL`).
 */
class CopyPropertyAsEnvVariableAction :
    CopyPropertyKeyActionBase(SpringCoreBundle.message("explyt.spring.properties.action.copy.env")) {

    override fun transform(fullKey: String): String = PropertyUtil.toEnvironmentVariableName(fullKey)
}
