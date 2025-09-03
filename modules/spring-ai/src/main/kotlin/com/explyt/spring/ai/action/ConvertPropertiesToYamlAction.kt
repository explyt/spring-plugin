/*
 * Copyright © 2025 Explyt Ltd
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
        val enabled = virtualFiles.any { it.fileType in YAML_TYPES }
        e.presentation.isEnabledAndVisible = enabled
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val yamlFiles = virtualFiles.filter {
            it.fileType in YAML_TYPES
        }
        val fileNames = yamlFiles.joinToString(", ") { it.name }

        val prompt = SpringAiBundle.message("action.prompt.convert.yaml", fileNames)
        AiPluginService.getInstance(project).performPrompt(prompt, yamlFiles)
    }

    companion object {
        private val YAML_TYPES = setOf(YAMLFileType.YML)
    }
}
