/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.action

import com.explyt.spring.ai.SpringAiBundle
import com.explyt.spring.ai.service.AiPluginService
import com.explyt.spring.core.util.ActionUtil
import com.intellij.lang.properties.PropertiesFileType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import org.jetbrains.yaml.YAMLFileType

class ConvertPropertiesToYamlAction : AnAction(SpringAiBundle.message("explyt.spring.ai.action.prop.to.yaml")) {
    override fun update(e: AnActionEvent) {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (virtualFiles == null || virtualFiles.size > 1) {
            ActionUtil.isEnabledAndVisible(e, false)
            return
        }
        val enabled = virtualFiles.any { it.fileType == PropertiesFileType.INSTANCE }
        e.presentation.isEnabledAndVisible = enabled
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val propertiesFiles = virtualFiles.filter {
            it.fileType == PropertiesFileType.INSTANCE
        }
        val fileNames = propertiesFiles.joinToString(", ") { it.name }

        val prompt = SpringAiBundle.message("action.prompt.convert.properties", fileNames)
        AiPluginService.getInstance(project).performPrompt(prompt, propertiesFiles)
    }
}

class ConvertYamlToPropertiesAction : AnAction(SpringAiBundle.message("explyt.spring.ai.action.yaml.to.prop")) {
    override fun update(e: AnActionEvent) {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (virtualFiles == null || virtualFiles.size > 1) {
            ActionUtil.isEnabledAndVisible(e, false)
            return
        }
        e.presentation.isEnabledAndVisible = virtualFiles.any { it.fileType == YAMLFileType.YML }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val yamlFiles = virtualFiles.filter { it.fileType == YAMLFileType.YML }
        val fileNames = yamlFiles.joinToString(", ") { it.name }

        val prompt = SpringAiBundle.message("action.prompt.convert.yaml", fileNames)
        AiPluginService.getInstance(project).performPrompt(prompt, yamlFiles)
    }
}
