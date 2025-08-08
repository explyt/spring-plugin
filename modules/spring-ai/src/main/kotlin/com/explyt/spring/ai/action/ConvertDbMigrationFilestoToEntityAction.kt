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
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText

class ConvertDbMigrationFilestoToEntityAction : AnAction(SpringAiBundle.message("explyt.spring.ai.action.db.to.jpa")) {
    override fun update(e: AnActionEvent) {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val files = getDbMigrationFiles(virtualFiles)
        if (files.size > 10) return

        val enabled = files.isNotEmpty()

        e.presentation.isEnabledAndVisible = enabled
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return

        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val files = getDbMigrationFiles(virtualFiles)

        val dtoNames = files.map { it.name }

        val prompt = SpringAiBundle.message("action.prompt.convert.db", dtoNames)
        //service.sendPromptWithClasses(prompt, dtoPsiClasses)
    }

    private fun getDbMigrationFiles(virtualFiles: Array<out VirtualFile>): List<VirtualFile> {
        val files = virtualFiles
            .filter { it.length < 1024 * 1024 } //1mb
            .filter {
                it.name.endsWith(".sql") || it.name.endsWith(".xml")
                        || it.name.endsWith(".yaml") || it.name.endsWith(".yml") || it.name.endsWith(".json")
            }
        return files.filter { it.readText().contains("table") }
    }
}